#### Сообщения от сервера

Chat message
```json
{
  "type": "message",
  "content": {
    "id": 52,
    "timestamp": 1499789037199,
    "highlighted": true,
    "author": "theauthor",
    "text": "@someuser emoticon ${!1} / linl ${!2} / image ${!3}",
    "source": "twitch",
    "elements": [
      {
        "type": "emoticon",
        "origin": "peka2tv",
        "code": "peka",
        "url": "http://peka2.tv/img/peka.png"
      },
      {
        "type": "link",
        "domain": "osu.ppy.sh",
        "fullUrl": "https://osu.ppy.sh/s/472567",
        "shortUrl": "osu.ppy.sh/s/472567"
      },
      {
        "type": "image",
        "url": "http://peka2.tv/logo.png"
      }
    ]
  }
}
```

Chat message
```json

```

Viewers message  
null в coutner'е означает что не удалось получить количество зрителей, но счётчик включён 
```json
{
  "type": "viewers", 
  "content": {
    "show": true,
    "counters": {
      "peka2tv": 100,
      "twitch": 200,
      "goodgame": null
    }
  }
}
```

Mod message
```json
{
  "type": "mod", 
  "content": {
    "messageId": 123
  }
}
```


#### Сообщения от клиента
Delete message
```json
{
  "type": "delete-message",
  "content": {
    "messageId": 123
  }
}
```


Ignore user
```json
{
  "type": "ignore",
  "content": {
    "user": "baduser#twitch",
    "messageId": 123
  }
}
```

Viewers count request
```json
{
  "type": "viewers",
  "content": {}
}
```




