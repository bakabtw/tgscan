package xyz.tgscan.service;

import static xyz.tgscan.enums.IdxConstant.MESSAGE_CONTENT_PHRASE;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Highlight;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.elasticsearch.core.search.SourceConfig;
import co.elastic.clients.elasticsearch.core.search.SourceFilter;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.util.ObjectBuilder;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import xyz.tgscan.dto.MessageDocDTO;
import xyz.tgscan.dto.QueryDTO;
import xyz.tgscan.dto.RoomDocDTO;
import xyz.tgscan.dto.SearchRespDTO;
import xyz.tgscan.enums.IdxConstant;
import xyz.tgscan.enums.TgRoomTypeParamEnum;

@Service
public class SearchService {
  private static final int PAGE_SIZE = 15;
  private static final double TERM_FILTER_THRESHOLD = 0.85;
  private static final String PRE_TAGS = "<span style='color:red'>";
  private static final String POST_TAGS = "</span>";
  private final ElasticsearchClient esClient;
  @Autowired private QueryProcessor queryProcessor;

  public SearchService(ElasticsearchClient esClient) {
    this.esClient = esClient;
  }

    private static Function<Query.Builder, ObjectBuilder<Query>> buildTagFilter0(QueryDTO query) {
        return t1 ->
                t1.terms(
                        TermsQuery.of(
                                t2 ->
                                        t2.field(IdxConstant.ROOM_TAGS)
                                                .terms(
                                                        t3 -> {
                                                            var tagsVals =
                                                                    query.getTags().stream()
                                                                            .map(z -> new FieldValue.Builder().stringValue(z).build())
                                                                            .collect(Collectors.toList());
                                                            return t3.value(tagsVals);
                                                        })));
    }
    private static Function<Query.Builder, ObjectBuilder<Query>> buildCategoryFilter(QueryDTO query) {
        return t1 ->
                t1.terms(
                        TermsQuery.of(
                                t2 ->
                                        t2.field(IdxConstant.ROOM_CATEGORY)
                                                .terms(
                                                        t3 -> {
                                                            var tagsVals =
                                                                    query.getTags().stream()
                                                                            .map(z -> new FieldValue.Builder().stringValue(z).build())
                                                                            .collect(Collectors.toList());
                                                            return t3.value(tagsVals);
                                                        })));
    }

  @SneakyThrows
  public SearchRespDTO recall(String kw, Integer page, TgRoomTypeParamEnum type) {

    var query = queryProcessor.process(kw);

    var esQuery = buildESQuery(type, query);
    var highLight = buildHighLight();

    // Calculate the offset for pagination
    var from = (page - 1) * PAGE_SIZE;

    // Create a builder to build the search request
    Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>> builder =
        s ->
            s.index(IdxConstant.ROOM_IDX, IdxConstant.MESSAGE_IDX)
                .size(PAGE_SIZE)
                .from(from)
                .query(esQuery)
                .source(
                    new SourceConfig.Builder()
                        .filter(
                            new SourceFilter.Builder()
                                .excludes(
                                    IdxConstant.ROOM_STANDARD_DESC,
                                    IdxConstant.ROOM_STANDARD_NAME,
                                    IdxConstant.ROOM_DESC_PHRASE,
                                    IdxConstant.MESSAGE_CONTENT_PHRASE)
                                .build())
                        .build())
                .highlight(highLight);

    // Create the search request builder
    SearchRequest.Builder requestBuilder = new SearchRequest.Builder();
    SearchRequest searchRequest = builder.apply(requestBuilder).build();

    // Execute the search request
    SearchResponse<Object> response = esClient.search(searchRequest, Object.class);

    // Get the hits from the response
    var hits = response.hits();
    var total = Objects.requireNonNull(hits.total()).value();
    var totalPage = total == PAGE_SIZE ? total / PAGE_SIZE : total / PAGE_SIZE + 1;
    return buildRespDTO(hits, totalPage);
  }

  private SearchRespDTO buildRespDTO(HitsMetadata<Object> hits, long totalPage) {
    var docs =
        hits.hits().stream()
            .map(
                tgRoomDocHit -> {
                  var index = tgRoomDocHit.index();
                  if (index.equals(IdxConstant.ROOM_IDX)) {
                    var source = (Map) tgRoomDocHit.source();
                    var name =
                        java.lang.String.join(
                            " ",
                            Optional.ofNullable(
                                    tgRoomDocHit.highlight().get(IdxConstant.ROOM_STANDARD_NAME))
                                .orElse(
                                    List.of(
                                        Objects.requireNonNull(source).get("name").toString())));
                    var jhiDesc =
                        java.lang.String.join(
                            " ",
                            Optional.ofNullable(
                                    tgRoomDocHit.highlight().get(IdxConstant.ROOM_STANDARD_DESC))
                                .orElse(
                                    List.of(
                                        Optional.ofNullable(source.get("jhiDesc"))
                                            .orElse("")
                                            .toString())));
                    return RoomDocDTO.fromTgRoomDoc(source, name, jhiDesc);
                  }
                  if (index.equals(IdxConstant.MESSAGE_IDX)) {
                    var source = (Map) tgRoomDocHit.source();
                    var title =
                        java.lang.String.join(
                            " ",
                            Optional.ofNullable(
                                    tgRoomDocHit.highlight().get(IdxConstant.MESSAGE_TITLE))
                                .orElse(
                                    List.of(
                                        Objects.requireNonNull(source).get("title").toString())));
                    var content =
                        java.lang.String.join(
                            " ",
                            Optional.ofNullable(
                                    tgRoomDocHit.highlight().get(IdxConstant.MESSAGE_CONTENT))
                                .orElse(List.of(source.get("content").toString())));
                    return MessageDocDTO.fromTgMessageDoc(source, title, content);
                  }

                  return null;
                })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    return new SearchRespDTO(totalPage, docs);
  }

  private Function<Highlight.Builder, ObjectBuilder<Highlight>> buildHighLight() {
    return h ->
        h.fields(
                IdxConstant.ROOM_STANDARD_NAME,
                f ->
                    f.matchedFields(IdxConstant.ROOM_STANDARD_NAME)
                        .preTags(PRE_TAGS)
                        .postTags(POST_TAGS)
                        .requireFieldMatch(false))
            .fields(
                IdxConstant.ROOM_STANDARD_DESC,
                f ->
                    f.matchedFields(IdxConstant.ROOM_STANDARD_DESC)
                        .preTags(PRE_TAGS)
                        .postTags(POST_TAGS)
                        .requireFieldMatch(false))
            .fields(
                IdxConstant.MESSAGE_TITLE,
                f ->
                    f.matchedFields(IdxConstant.MESSAGE_TITLE)
                        .preTags(PRE_TAGS)
                        .requireFieldMatch(false)
                        .postTags(POST_TAGS))
            .fields(
                IdxConstant.MESSAGE_CONTENT,
                f ->
                    f.matchedFields(IdxConstant.MESSAGE_CONTENT)
                        .preTags(PRE_TAGS)
                        .requireFieldMatch(false)
                        .fragmentSize(3000)
                        .postTags(POST_TAGS));
  }

  private Function<Query.Builder, ObjectBuilder<Query>> buildESQuery(
      TgRoomTypeParamEnum type, QueryDTO query) {

    // tg room

    var kw = query.getKw();
    Query termByUsername =
        TermQuery.of(
                m ->
                    m.field(IdxConstant.ROOM_USERNAME)
                        .value(kw.startsWith("@") ? kw.substring(1) : kw)
                        .boost(100f))
            ._toQuery();
    Query matchByName =
        MatchQuery.of(m -> m.field(IdxConstant.ROOM_NAME).query(kw).boost(2f))._toQuery();
    Query matchByStandardName =
        MatchQuery.of(m -> m.field(IdxConstant.ROOM_STANDARD_NAME).query(kw).boost(2f))._toQuery();
    Query matchByDesc =
        MatchQuery.of(r -> r.field(IdxConstant.ROOM_DESC).query(kw).boost(1f))._toQuery();
    Query matchByStandardDesc =
        MatchQuery.of(r -> r.field(IdxConstant.ROOM_STANDARD_DESC).query(kw).boost(1f))._toQuery();

    // tg message
    Query matchByTitle =
        MatchQuery.of(m -> m.field(IdxConstant.MESSAGE_TITLE).query(kw).boost(2f))._toQuery();
    Query matchByDesc0 =
        MatchQuery.of(r -> r.field(IdxConstant.MESSAGE_CONTENT).query(kw).boost(1f))._toQuery();

    var termByName =
        query.getTermWeight().entrySet().stream()
            .map(
                x ->
                    TermQuery.of(
                            m ->
                                m.field(IdxConstant.ROOM_NAME)
                                    .value(x.getKey())
                                    .boost(x.getValue() * 3f))
                        ._toQuery())
            .toList();
    var termByDesc =
        query.getTermWeight().entrySet().stream()
            .map(
                x ->
                    TermQuery.of(
                            m ->
                                m.field(IdxConstant.ROOM_DESC)
                                    .value(x.getKey())
                                    .boost(x.getValue() * 1f))
                        ._toQuery())
            .toList();
    Query matchPhraseByName =
        MatchPhraseQuery.of(m -> m.field(IdxConstant.ROOM_NAME).query(kw).slop(3).boost(6f))
            ._toQuery();
    Query matchPhraseByStandardName =
        MatchPhraseQuery.of(
                m -> m.field(IdxConstant.ROOM_STANDARD_NAME).query(kw).slop(3).boost(6f))
            ._toQuery();
    Query matchPhraseByDesc =
        MatchPhraseQuery.of(r -> r.field(IdxConstant.ROOM_DESC_PHRASE).query(kw).slop(3).boost(1f))
            ._toQuery();
    Query matchPhraseByStandardDesc =
        MatchPhraseQuery.of(
                r -> r.field(IdxConstant.ROOM_STANDARD_DESC).query(kw).slop(3).boost(1f))
            ._toQuery();

    // tg message
    var termByTitle =
        query.getTermWeight().entrySet().stream()
            .map(
                x ->
                    TermQuery.of(
                            m ->
                                m.field(IdxConstant.MESSAGE_TITLE)
                                    .value(x.getKey())
                                    .boost(x.getValue() * 3f))
                        ._toQuery())
            .toList();
    var termByDesc0 =
        query.getTermWeight().entrySet().stream()
            .map(
                x ->
                    TermQuery.of(
                            m ->
                                m.field(IdxConstant.MESSAGE_CONTENT)
                                    .value(x.getKey())
                                    .boost(x.getValue() * 1f))
                        ._toQuery())
            .toList();
    Query matchPhraseByTitle =
        MatchPhraseQuery.of(m -> m.field(IdxConstant.MESSAGE_TITLE).query(kw).slop(3).boost(6f))
            ._toQuery();
    Query matchPhraseByDesc0 =
        MatchPhraseQuery.of(r -> r.field(MESSAGE_CONTENT_PHRASE).query(kw).slop(3).boost(2f))
            ._toQuery();
    var titleFilter =
        query.getTermWeight().entrySet().stream()
            .filter(x -> x.getValue() >= TERM_FILTER_THRESHOLD)
            .flatMap(
                x ->
                    Stream.of(
                        TermQuery.of(m -> m.field(IdxConstant.MESSAGE_TITLE).value(x.getKey()))
                            ._toQuery(),
                        TermQuery.of(m -> m.field(IdxConstant.ROOM_NAME).value(x.getKey()))
                            ._toQuery()))
            .toList();
    var contentFilter =
        query.getTermWeight().entrySet().stream()
            .filter(x -> x.getValue() >= TERM_FILTER_THRESHOLD)
            .flatMap(
                x ->
                    Stream.of(
                        TermQuery.of(m -> m.field(IdxConstant.MESSAGE_CONTENT).value(x.getKey()))
                            ._toQuery(),
                        TermQuery.of(m -> m.field(IdxConstant.ROOM_DESC).value(x.getKey()))
                            ._toQuery()))
            .toList();
    return q ->
        q.functionScore(
            f ->
                f.query(
                        q0 ->
                            q0.bool(
                                b1 -> {
                                  if (type != TgRoomTypeParamEnum.ALL) {
                                    b1.filter(
                                        f2 ->
                                            f2.bool(
                                                b2 -> {
                                                  var typeFilter =
                                                      b2.must(
                                                          m1 ->
                                                              m1.term(
                                                                  t1 ->
                                                                      t1.field(
                                                                              IdxConstant.ROOM_TYPE)
                                                                          .value(type.name())));
                                                  if (!query.getTags().isEmpty()) {

                                                    b2.must(
                                                        m1 ->
                                                            m1.bool(
                                                                b3 -> b3.should(
                                                                        buildTagFilter0(query)).should(buildCategoryFilter(query))));
                                                  }
                                                  return typeFilter;
                                                }));
                                  }
                                  return b1.must(
                                      x1 ->
                                          x1.bool(
                                              y1 -> {
                                                termByName.forEach(y1::should);
                                                termByTitle.forEach(y1::should);
                                                termByDesc0.forEach(y1::should);
                                                termByDesc.forEach(y1::should);
                                                return y1.should(matchPhraseByName)
                                                    .should(matchPhraseByDesc)
                                                    .should(matchPhraseByStandardName)
                                                    .should(matchPhraseByStandardDesc)
                                                    .should(matchPhraseByTitle)
                                                    .should(matchPhraseByDesc0)
                                                    .should(matchByName)
                                                    .should(termByUsername)
                                                    .should(matchByStandardName)
                                                    .should(matchByDesc0)
                                                    .should(matchByDesc)
                                                    .should(matchByStandardDesc)
                                                    .should(matchByTitle);
                                              }));
                                }))
                    .functions(
                        f0 ->
                            f0.filter(
                                    f1 ->
                                        f1.bool(
                                            f2 -> {
                                              titleFilter.forEach(f2::should);
                                              return f2.should(matchPhraseByName);
                                            }))
                                .fieldValueFactor(
                                    f1 ->
                                        f1.field(IdxConstant.ROOM_MEMBER_CNT)
                                            .factor(1.0)
                                            .modifier(FieldValueFactorModifier.Log1p)
                                            .missing(1.0))
                                .weight(15.0))
                    .functions(
                        f0 ->
                            f0.filter(
                                    f1 ->
                                        f1.bool(
                                            f2 -> {
                                              contentFilter.forEach(f2::should);
                                              return f2.should(matchPhraseByDesc);
                                            }))
                                .fieldValueFactor(
                                    f1 ->
                                        f1.field(IdxConstant.ROOM_MEMBER_CNT)
                                            .factor(1.0)
                                            .modifier(FieldValueFactorModifier.Log1p)
                                            .missing(1.0))
                                .weight(5.0))
                    .functions(
                        f0 ->
                            f0.gauss(
                                    g ->
                                        g.placement(
                                                p ->
                                                    p.origin(JsonData.of("now"))
                                                        .offset(JsonData.of("365d"))
                                                        .decay(0.5)
                                                        .scale(JsonData.of("1d")))
                                            .field(IdxConstant.MESSAGE_SEND_TIME))
                                .weight(30.))
                    .scoreMode(FunctionScoreMode.Sum)
                    .boostMode(FunctionBoostMode.Sum));
  }
}
