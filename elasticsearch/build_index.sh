#!/bin/bash

/usr/share/elasticsearch/bin/elasticsearch-plugin install --batch https://github.com/medcl/elasticsearch-analysis-pinyin/releases/download/v7.17.6/elasticsearch-analysis-pinyin-7.17.6.zip
/usr/share/elasticsearch/bin/elasticsearch-plugin install --batch https://github.com/medcl/elasticsearch-analysis-ik/releases/download/v7.17.6/elasticsearch-analysis-ik-7.17.6.zip
/usr/share/elasticsearch/bin/elasticsearch-plugin install --batch https://github.com/medcl/elasticsearch-analysis-stconvert/releases/download/v7.17.6/elasticsearch-analysis-stconvert-7.17.6.zip

curl -XPUT "http://localhost:9200/message.0708" -H "Content-type: application/json" -d'
{
  "settings": {
    "index": {
      "refresh_interval": "120s",
      "translog.durability": "async",
      "translog.sync_interval": "120s",
      "translog.flush_threshold_size": "512mb",
      "number_of_shards": 1,
      "number_of_replicas": 0,
      "analysis": {
        "tokenizer": {
          "my_ik_smart": {
            "type": "ik_smart",
            "enable_lowercase": false
          }
        },
        "filter": {
          "tsconvert": {
            "convert_type": "t2s",
            "type": "stconvert",
            "keep_both": "false"
          },
          "my_word_delimiter_filter": {
            "type": "word_delimiter",
            "type_table": [
              "- => ALPHA"
            ],
            "split_on_case_change": true,
            "split_on_numerics": false,
            "stem_english_possessive": true,
            "preserve_original": true
          }
        },
        "analyzer": {
          "index_analyzer": {
            "filter": [
              "lowercase",
              "tsconvert"
            ],
            "tokenizer": "ik_max_word"
          },
          "phrase_index_analyzer": {
            "filter": [
              "lowercase",
              "tsconvert"
            ],
            "tokenizer": "my_ik_smart"
          },
          "search_analyzer": {
            "filter": [
              "my_word_delimiter_filter",
              "tsconvert",
              "lowercase"
            ],
            "tokenizer": "my_ik_smart"
          }
        }
      }
    }
  },
  "mappings": {
    "dynamic": false,
    "properties": {
      "id": {
        "type": "keyword"
      },
      "offsetId": {
        "type": "keyword"
      },
      "chatId": {
        "type": "keyword"
      },
      "tags": {
        "type": "keyword"
      },
      "sendTime": {
        "type": "date"
      },
      "type": {
        "type": "keyword"
      },
      "title": {
        "type": "text",
        "analyzer": "index_analyzer",
        "search_analyzer": "search_analyzer"
      },
      "content": {
        "type": "text",
        "analyzer": "index_analyzer",
        "search_analyzer": "search_analyzer"
      },
      "phrase_title": {
        "type": "text",
        "analyzer": "phrase_index_analyzer",
        "search_analyzer": "search_analyzer"
      },
      "phrase_content": {
        "type": "text",
        "analyzer": "phrase_index_analyzer",
        "search_analyzer": "search_analyzer"
      }
    }
  }
}'

curl -XPUT "http://localhost:9200/room.0719" -H "Content-type: application/json" -d'
{
  "settings": {
    "index": {
      "refresh_interval": "120s",
      "translog.durability": "async",
      "translog.sync_interval": "120s",
      "translog.flush_threshold_size": "512mb",
      "number_of_shards": 1,
      "number_of_replicas": 0,
      "analysis": {
        "tokenizer": {
          "my_ik_smart": {
            "type": "ik_smart",
            "enable_lowercase": false
          }
        },
        "filter": {
          "tsconvert": {
            "convert_type": "t2s",
            "type": "stconvert",
            "keep_both": "false"
          },
          "my_word_delimiter_filter": {
            "type": "word_delimiter",
            "type_table": [
              "- => ALPHA"
            ],
            "split_on_case_change": true,
            "split_on_numerics": false,
            "stem_english_possessive": true,
            "preserve_original": true
          }
        },
        "analyzer": {
          "index_analyzer": {
            "filter": [
              "lowercase",
              "tsconvert"
            ],
            "tokenizer": "ik_max_word"
          },
          "phrase_index_analyzer": {
            "filter": [
              "lowercase",
              "tsconvert"
            ],
            "tokenizer": "my_ik_smart"
          },
          "search_analyzer": {
            "filter": [
              "my_word_delimiter_filter",
              "tsconvert",
              "lowercase"
            ],
            "tokenizer": "my_ik_smart"
          }
        }
      }
    }
  },
  "mappings": {
    "dynamic": false,
    "properties": {
      "id": {
        "type": "keyword"
      },
      "link": {
        "type": "keyword"
      },
      "userName": {
        "type": "keyword"
      },
      "memberCnt": {
        "type": "long"
      },
      "icon": {
        "type": "keyword"
      },
      "type": {
        "type": "keyword"
      },
      "status": {
        "type": "keyword"
      },
      "name": {
        "type": "text",
        "analyzer": "index_analyzer",
        "search_analyzer": "search_analyzer"
      },
      "jhiDesc": {
        "type": "text",
        "analyzer": "index_analyzer",
        "search_analyzer": "search_analyzer"
      },
      "phraseName": {
        "type": "text",
        "analyzer": "phrase_index_analyzer",
        "search_analyzer": "search_analyzer"
      },
      "standardName": {
        "type": "text",
        "analyzer": "standard",
        "search_analyzer": "standard"
      },
      "standardJhiDesc": {
        "type": "text",
        "analyzer": "standard",
        "search_analyzer": "standard"
      },
      "phraseJhiDesc": {
        "type": "text",
        "analyzer": "phrase_index_analyzer",
        "search_analyzer": "search_analyzer"
      },
      "sendTime": {
        "type": "date"
      }
    }
  }
}'
