import os
import logging

import asyncio
import socks
from telethon import TelegramClient

api_id = int(os.environ.get("API_ID"))
api_hash = os.environ.get("API_HASH")
phone = os.environ.get("PHONE")
proxy_host = os.environ.get("PROXY_HOST")
proxy_port = int(os.environ.get("PROXY_PORT"))
proxy_username = os.environ.get("PROXY_USERNAME")
proxy_password = os.environ.get("PROXY_PASSWORD")

proxy = None
use_proxy = os.environ.get("USE_PROXY", "False").lower() == "true"
if use_proxy:
    proxy = (socks.SOCKS5, proxy_host, proxy_port, True, proxy_username, proxy_password)

async def main():
    if not os.path.exists(f"sessions/{phone}.session"):
        logging.info("Creating a new sessions.")
        client = TelegramClient(f"sessions/{phone}", api_id, api_hash, proxy=proxy)
        await client.start(phone=phone)
    else:
        logging.error("The session already exists.")

if __name__ == '__main__':
    asyncio.run(main())
