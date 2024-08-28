FROM elasticsearch:7.17.6 AS esbuilder

COPY --chmod=0755 build_index.sh /usr/local/bin/build_index.sh

RUN apt-get update \
    && apt-get install procps -y \
    && apt-get install httping -y \
    && /usr/local/bin/docker-entrypoint.sh elasticsearch -d -E path.data=/tmp/data -E cluster.name=elasticsearch -E discovery.type=single-node \
    && while ! httping -qc1 http://localhost:9200 ; do sleep 1 ; done \
    && /usr/local/bin/build_index.sh \
    && apt-get clean


FROM elasticsearch:7.17.6

COPY --from=esbuilder /tmp/data/ /usr/share/elasticsearch/data/
