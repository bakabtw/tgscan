FROM elasticsearch:7.17.6

RUN /usr/share/elasticsearch/bin/elasticsearch-plugin install --batch https://github.com/medcl/elasticsearch-analysis-pinyin/releases/download/v7.17.6/elasticsearch-analysis-pinyin-7.17.6.zip
RUN /usr/share/elasticsearch/bin/elasticsearch-plugin install --batch https://github.com/medcl/elasticsearch-analysis-ik/releases/download/v7.17.6/elasticsearch-analysis-ik-7.17.6.zip
RUN /usr/share/elasticsearch/bin/elasticsearch-plugin install --batch https://github.com/medcl/elasticsearch-analysis-stconvert/releases/download/v7.17.6/elasticsearch-analysis-stconvert-7.17.6.zip

COPY --chmod=0755 build_index.sh /usr/local/bin/
