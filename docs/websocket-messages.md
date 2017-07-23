#### Сообщения от сервера

Chat message - сообщение из чата
```json
{
  "type": "message",
  "content": {
    "id": 52,
    "timestamp": 1499789037199,
    "highlighted": true,
    "author": "theauthor",
    "text": "@someuser emoticon ${!0} / linl ${!1} / image ${!2}",
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

Viewers message - текущие подключённые источники и количество зрителей на их каналах.
- null в coutner'е означает что не удалось получить количество зрителей, но счётчик включён 
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

Delete message - удаление сообщения из чата
```json
{
  "type": "delete-message", 
  "content": {
    "messageId": 123
  }
}
```


#### Сообщения от клиента
Delete message - запрос клиента на удаление сообщения
```json
{
  "type": "delete-message",
  "content": {
    "messageId": 123
  }
}
```


Ignore user - запрос клиента на блокировку юзера удаление сообщения
```json
{
  "type": "ignore-user",
  "content": {
    "user": "baduser#twitch",
    "messageId": 123
  }
}
```

Viewers count request - запрос клиента на получение от сервера сообщения типа 'viewers' 
```json
{
  "type": "viewers",
  "content": {}
}
```
