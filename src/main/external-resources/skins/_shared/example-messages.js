$(() => {

    failchat.nativeClient = true;

    failchat.handleMessage({
        "type": "client-configuration",
        "content": {
            "statusMessageMode": "everywhere",
            "showViewersCount": true,
            "nativeClientBgColor": "#000000ff",
            // "nativeClientBgColor": "#ffffffff",
            "externalClientBgColor": "#000000ff",
            "enabledOrigins": {
                "peka2tv": true,
                "goodgame": true,
                "twitch": true,
                "youtube": true,
                "cybergame": true
            }
        }
    });

    failchat.handleMessage(
        {
            "type": "viewers-count",
            "content": {
                "peka2tv": 5,
                "goodgame": 25,
                "twitch": 250,
                "youtube": 2514,
                "cybergame": null
            }
        }
    );

    failchat.handleMessage(
        {"type": "origin-status", "content": {"origin": "failchat", "status": "connected", "timestamp": Date.now()}}
    );

    failchat.handleMessage({
        "type": "message",
        "content": {
            "id": 39311,
            "origin": "twitch",
            "author": {"name": "fail0001", "id": "fail0001"},
            "text": "hello",
            "timestamp": 1529402393227,
            "highlighted": false,
            "badges": [{
                "type": "image",
                "format": "raster",
                "url": "https://static-cdn.jtvnw.net/badges/v1/5527c58c-fb7d-422d-b71b-f309dcb85cc1/2",
                "description": "Broadcaster"
            }],
            "elements": []
        }
    });

    failchat.handleMessage({
        "type": "message",
        "content": {
            "id": 39312,
            "origin": "peka2tv",
            "author": {"name": "fail0001", "id": "fail0001"},
            "text": "{!0}",
            "timestamp": 1529402395322,
            "highlighted": false,
            "badges": [{
                "type": "image",
                "format": "raster",
                "url": "https://static-cdn.jtvnw.net/badges/v1/5527c58c-fb7d-422d-b71b-f309dcb85cc1/2",
                "description": "Broadcaster"
            }],
            "elements": [{
                "type": "emoticon",
                "code": "Kappa",
                "url": "http://static-cdn.jtvnw.net/emoticons/v1/25/1.0",
                "format": "raster"
            }]
        }
    });

    failchat.handleMessage({
        "type": "message",
        "content": {
            "id": 39313,
            "origin": "youtube",
            "author": {"name": "fail0001", "id": "fail0001"},
            "text": "123 {!0} 123 {!1} 123 {!2} 123 {!3} 123 {!4} 123 {!5} 123 {!6} 123 {!7} 123 {!8} 123 {!9} 123 {!10} 123 {!11} 123 {!12} 123 {!13} 123 {!14} 123 {!15} 123 {!16} 123 {!17}",
            "timestamp": 1529402406269,
            "highlighted": false,
            "badges": [{
                "type": "image",
                "format": "raster",
                "url": "https://static-cdn.jtvnw.net/badges/v1/5527c58c-fb7d-422d-b71b-f309dcb85cc1/2",
                "description": "Broadcaster"
            }],
            "elements": [{
                "type": "emoticon",
                "code": "Kappa",
                "url": "http://static-cdn.jtvnw.net/emoticons/v1/25/1.0",
                "format": "raster"
            }, {
                "type": "emoticon",
                "code": "Kappa",
                "url": "http://static-cdn.jtvnw.net/emoticons/v1/25/1.0",
                "format": "raster"
            }, {
                "type": "emoticon",
                "code": "Kappa",
                "url": "http://static-cdn.jtvnw.net/emoticons/v1/25/1.0",
                "format": "raster"
            }, {
                "type": "emoticon",
                "code": "Kappa",
                "url": "http://static-cdn.jtvnw.net/emoticons/v1/25/1.0",
                "format": "raster"
            }, {
                "type": "emoticon",
                "code": "Kappa",
                "url": "http://static-cdn.jtvnw.net/emoticons/v1/25/1.0",
                "format": "raster"
            }, {
                "type": "emoticon",
                "code": "Kappa",
                "url": "http://static-cdn.jtvnw.net/emoticons/v1/25/1.0",
                "format": "raster"
            }, {
                "type": "emoticon",
                "code": "Kappa",
                "url": "http://static-cdn.jtvnw.net/emoticons/v1/25/1.0",
                "format": "raster"
            }, {
                "type": "emoticon",
                "code": "Kappa",
                "url": "http://static-cdn.jtvnw.net/emoticons/v1/25/1.0",
                "format": "raster"
            }, {
                "type": "emoticon",
                "code": "Kappa",
                "url": "http://static-cdn.jtvnw.net/emoticons/v1/25/1.0",
                "format": "raster"
            }, {
                "type": "emoticon",
                "code": "Kappa",
                "url": "http://static-cdn.jtvnw.net/emoticons/v1/25/1.0",
                "format": "raster"
            }, {
                "type": "emoticon",
                "code": "Kappa",
                "url": "http://static-cdn.jtvnw.net/emoticons/v1/25/1.0",
                "format": "raster"
            }, {
                "type": "emoticon",
                "code": "Kappa",
                "url": "http://static-cdn.jtvnw.net/emoticons/v1/25/1.0",
                "format": "raster"
            }, {
                "type": "emoticon",
                "code": "Kappa",
                "url": "http://static-cdn.jtvnw.net/emoticons/v1/25/1.0",
                "format": "raster"
            }, {
                "type": "emoticon",
                "code": "Kappa",
                "url": "http://static-cdn.jtvnw.net/emoticons/v1/25/1.0",
                "format": "raster"
            }, {
                "type": "emoticon",
                "code": "Kappa",
                "url": "http://static-cdn.jtvnw.net/emoticons/v1/25/1.0",
                "format": "raster"
            }, {
                "type": "emoticon",
                "code": "Kappa",
                "url": "http://static-cdn.jtvnw.net/emoticons/v1/25/1.0",
                "format": "raster"
            }, {
                "type": "emoticon",
                "code": "Kappa",
                "url": "http://static-cdn.jtvnw.net/emoticons/v1/25/1.0",
                "format": "raster"
            }, {
                "type": "emoticon",
                "code": "Kappa",
                "url": "http://static-cdn.jtvnw.net/emoticons/v1/25/1.0",
                "format": "raster"
            }]
        }
    });

    failchat.handleMessage({
        "type": "message",
        "content": {
            "id": 39314,
            "origin": "cybergame",
            "author": {"name": "fail0001", "id": "fail0001"},
            "text": "{!0}",
            "timestamp": 1529402443689,
            "highlighted": false,
            "badges": [{
                "type": "image",
                "format": "raster",
                "url": "https://static-cdn.jtvnw.net/badges/v1/5527c58c-fb7d-422d-b71b-f309dcb85cc1/2",
                "description": "Broadcaster"
            }],
            "elements": [{"type": "image", "url": "https://i.imgur.com/tCGN8Zj.png"}]
        }
    });

    failchat.handleMessage({
        "type": "message",
        "content": {
            "id": 39315,
            "origin": "goodgame",
            "author": {"name": "fail0001", "id": "fail0001"},
            "text": "test image {!1} 123ffff {!0}",
            "timestamp": 1529402450519,
            "highlighted": false,
            "badges": [{
                "type": "image",
                "format": "raster",
                "url": "https://static-cdn.jtvnw.net/badges/v1/5527c58c-fb7d-422d-b71b-f309dcb85cc1/2",
                "description": "Broadcaster"
            }],
            "elements": [{
                "type": "emoticon",
                "code": "Kappa",
                "url": "http://static-cdn.jtvnw.net/emoticons/v1/25/1.0",
                "format": "raster"
            }, {"type": "image", "url": "https://i.imgur.com/tCGN8Zj.png"}]
        }
    });

    failchat.handleMessage({
        "type": "message",
        "content": {
            "id": 39316,
            "origin": "twitch",
            "author": {"name": "fail0001", "id": "fail0001"},
            "text": "hello Kappa",
            "timestamp": 1529402475110,
            "highlighted": false,
            "badges": [
                {
                    "type": "character",
                    "htmlEntity": "&#59730;",
                    "color": "#eefc08"
                }
            ],
            "elements": []
        }
    });

    failchat.handleMessage({
        "type": "message",
        "content": {
            "id": 39317,
            "origin": "peka2tv",
            "author": {"name": "fail0001", "id": "fail0001"},
            "text": "should be deleted!",
            "timestamp": 1529402475110,
            "highlighted": true,
            "badges": [],
            "elements": []
        }
    });

    failchat.handleMessage(
        {
            "type": "delete-message",
            "content": {
                "messageId": 39317
            }
        }
    );

});