# Сообщения от сервера

#### Chat message 
Сообщение из чата
```json
{
    "type": "message",
    "content": {
        "id": 52,
        "timestamp": 1499789037199,
        "author": {
            "name": "theauthor",
            "id": "abc123"
        },
        "text": "@someuser emoticon ${!0} / link ${!1} / image ${!2}",
        "origin": "twitch",
        "highlighted": true,
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

#### Информационное сообщение
Сюда входят сообщения об подключение и отключение к источникам  
`content.status` может принимать значения: `connected`, `disconnected`  
`content.mode` может принимать значения: `everywhere`, `native_client`
```json
{
    "type": "origin-status",
    "content": {
        "origin": "twitch",
        "status": "connected",
        "timestamp": 1499789037199,
        "mode": "everywhere"
    }
}
```

#### Viewers message
Текущие подключённые источники и количество зрителей на их каналах. 
`null` означает что не удалось получить количество зрителей, но счётчик включён 
```json
{
    "type": "viewers-count",
    "content": {
        "peka2tv": 100,
        "twitch": 200,
        "goodgame": null
    }
}
```


#### Show viewers count
```json
{
    "type": "show-viewers-count",
    "content": {
        "show": true
    }
}
```

#### Enabled-origins
```json
{
    "type": "enabled-origins",
    "content": {
        "peka2tv": false,
        "twitch": true,
        "goodgame": true,
        "youtube": false      
    }
}
```

#### Delete message 
Удаление сообщения из чата
```json
{
    "type": "delete-message",
    "content": {
        "messageId": 123
    }
}
```




# Сообщения от клиента

#### Delete message
Запрос клиента на удаление сообщения
```json
{
    "type": "delete-message",
    "content": {
        "messageId": 123
    }
}
```


#### Ignore user
Запрос клиента на блокировку сообщений от пользователя
```json
{
    "type": "ignore-author",
    "content": {
        "authorId": "badauthor#twitch"
    }
}
```

#### Viewers count request  
```json
{
    "type": "viewers-count",
    "content": {}
}
```

#### Show viewers count request 
```json
{
    "type": "show-viewers-count",
    "content": {}
}
```


#### Enabled-origins
```json
{
    "type": "enabled-origins",
    "content": {}
}
```

