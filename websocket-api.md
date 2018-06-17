# Сообщения от сервера

#### Chat message 
Сообщение из чата   
`content.badges.type: "image" | "character"`  
`content.badges.description: String | null`
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
                "url": "http://peka2.tv/img/peka.png",
                "format": "raster"
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
        ],
        "badges": [
            {
                "type": "image",
                "url": "https://example.com/some-image.png",
                "description": "example badge"
            },
            {
                "type": "character",
                "htmlEntity": "&#59730;",
                "color": "#eefc08"
            }
        ]
    }
}
```

#### Client configuration
`status-message-mode: "everywhere" | "native_client" | "nowhere"`
```json
{
    "type": "client-configuration",
    "content": {
        "statusMessageMode": "everywhere",
        "showViewersCount": true,
        "nativeClientBgColor": "0x000000ff",
        "externalClientBgColor": "0x00000000",
        "enabledOrigins": {
            "peka2tv": false,
            "twitch": true,
            "goodgame": true,
            "youtube": false
        }
    }
}
```

#### Origin status
Сюда входят сообщения об подключение и отключение к источникам  
`content.status: "connected" | "disconnected"`  
```json
{
    "type": "origin-status",
    "content": {
        "origin": "twitch",
        "status": "connected",
        "timestamp": 1499789037199
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

#### Client configuration request 
```json
{
    "type": "client-configuration",
    "content": {}
}
```

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
