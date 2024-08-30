# Prerequsites
- Install docker (Ubuntu >=24.04)
```bash
apt update
apt install -y docker.io docker-compose-v2
```

- Clone the repo
```bash
git clone https://github.com/bakabtw/tgscan
```

- Change the dir
```bash
cd tgscan/
```

# First start
- Obtain `API_ID` and `API_HASH` from https://my.telegram.org

- Copy `.env.template` to `.env`
```bash
cp .env.template .env
```

- Make necessary changes in `.env`
    - Parameters you need to change:
        - `API_ID` - API ID obtained from https://my.telegram.org
        - `API_HASH` - API hash obtained from https://my.telegram.org
        - `PHONE` - account phone number
    - Optional parameters (change for prod environment)
        - `PG_DATABASE` - postgresql DB name
        - `PG_USER` - postgresql user
        - `PG_PASSWORD` - postgresql password
        - `PG_HOST` - postgresql host. Change it in case of using an external instance
        - `PG_PORT` - postgresql port
    - Proxy parameters
        - `USE_PROXY` - enable/disable proxy for TelegramClient (values: `'True'` or `'False'`)
        - `PROXY_HOST` - socks5 proxy host
        - `PROXY_PORT` - socks5 proxy port
        - `PROXY_USERNAME` - socks5 proxy username
        - `PROXY_PASSWORD` - socks5 proxy password
    - Change these only if you know what you're doing
        - `SPRING_PROFILES_ACTIVE` - environment for `api-server` (values: `prod` or `dev`)
        - `SPRING_DATASOURCE_URL` - postgresql jdbc string
        - `SPRING_ELASTICSEARCH_URIS` - elasticsearch URL. Change it in case of using an external instance

- Start containers
```bash
docker compose up -d
```

By that point, docker will automatically download and start all containers.

# Further settings
- Create elasticsearch indexes
```
docker exec -ti elasticsearch /usr/local/bin/build_index.sh
```

Correct response:
```bash
$ docker exec -ti elasticsearch /usr/local/bin/build_index.sh

{"acknowledged":true,"shards_acknowledged":true,"index":"message.0708"}
{"acknowledged":true,"shards_acknowledged":true,"index":"room.0719"
```

- Authorize in telegram
```
docker exec -ti msg-scraper python3 auth.py
```
