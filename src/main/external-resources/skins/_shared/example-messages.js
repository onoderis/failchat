$(() => {
    awaitForTemplates().then(() => {
        sendExampleMessages()
    });
});


function sendExampleMessages() {

    failchat.nativeClient = true;

    failchat.handleMessage({
        "type": "client-configuration",
        "content": {
            "showViewersCount": true,
            "showOriginBadges": true,
            "showUserBadges": true,
            "zoomPercent": 100,
            "hideDeletedMessages" : false,
            "showHiddenMessages": false,
            "nativeClient": {
                "backgroundColor": "#333333ff",
                "coloredNicknames": true,
                "hideMessages" : false,
                "hideMessagesAfter" : 60,
                "showStatusMessages" : true
            },
            "externalClient": {
                "backgroundColor": "#333333ff",
                "coloredNicknames": true,
                "hideMessages" : true,
                "hideMessagesAfter" : 20,
                "showStatusMessages" : false
            },
            "enabledOrigins": {
                "peka2tv": false,
                "twitch": true,
                "goodgame": true,
                "youtube": false
            },
            "deletedMessagePlaceholder": {
                "text": "message deleted {!0}",
                "elements": [
                    {
                        "type": "emoticon",
                        "code": "Kappa",
                        "url": "https://static-cdn.jtvnw.net/emoticons/v1/25/2.0",
                        "format": "raster"
                    }
                ]
            }
        }
    });

    failchat.handleMessage({
        "type": "viewers-count",
        "content": {
            "peka2tv": 5,
            "goodgame": 25,
            "twitch": 250,
            "youtube": 2514
        }
    });


    sendMessageAfterTimeout()

}

let lastMessageIndex = 0;
let lastId = 1;

function sendMessageAfterTimeout() {
    setTimeout(() => {
        if (lastMessageIndex >= exampleMessages.length) {
            lastMessageIndex = 0
        }

        exampleMessages[lastMessageIndex].content.id = lastId;
        failchat.handleMessage(exampleMessages[lastMessageIndex]);

        lastMessageIndex++;
        lastId++;

        sendMessageAfterTimeout();
    }, 1500)
}

const exampleMessages = [
    {
        "type": "message",
        "content": {
            "id": 1,
            "origin": "twitch",
            "author": {"name": "fail0001", "id": "fail0001", "color": "#00ff00"},
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
    },

    {
        "type": "message",
        "content": {
            "id": 2,
            "origin": "peka2tv",
            "author": {"name": "fail0001", "id": "fail0001", "color": null},
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
    },

    {
        "type": "message",
        "content": {
            "id": 3,
            "origin": "youtube",
            "author": {"name": "fail0001", "id": "fail0001", "color": null},
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
    },

    {
        "type": "message",
        "content": {
            "id": 5,
            "origin": "goodgame",
            "author": {"name": "fail0001", "id": "fail0001", "color": null},
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
    },

    {
        "type": "message",
        "content": {
            "id": 6,
            "origin": "twitch",
            "author": {"name": "fail0001", "id": "fail0001", "color": null},
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
    },

    {
        "type": "message",
        "content": {
            "id": 7,
            "origin": "peka2tv",
            "author": {"name": "fail0001", "id": "fail0001", "color": null},
            "text": "should be deleted!",
            "timestamp": 1529402475110,
            "highlighted": true,
            "badges": [],
            "elements": []
        }
    },

    {
        "type": "delete-message",
        "content": {
            "messageId": 7
        }
    },

    {
        "type": "message",
        "content": {
            "id": 8,
            "origin": "peka2tv",
            "author": {"name": "fail0001", "id": "fail0001", "color": null},
            "text": "so many badges!",
            "timestamp": 1529402475110,
            "highlighted": false,
            "badges": [
                {
                    "type": "character",
                    "htmlEntity": "&#59730;",
                    "color": "#eefc08"
                },
                {
                    "type": "character",
                    "htmlEntity": "&#59730;",
                    "color": "#eefc08"
                },
                {
                    "type": "character",
                    "htmlEntity": "&#59730;",
                    "color": "#eefc08"
                }
            ],
            "elements": []
        }
    },

    // GG badges
    {
        "type": "message",
        "content": {
            "id": 9,
            "origin": "goodgame",
            "author": {"name": "fail0001", "id": "fail0001", "color": null},
            "text": "badges test",
            "timestamp": 1529402475110,
            "highlighted": false,
            "badges": [
                {
                    "type": "character",
                    "htmlEntity": "&#59730;",
                    "color": "#eefc08"
                },
                {
                    "type": "character",
                    "htmlEntity": "&#59729;",
                    "color": "#eefc08"
                }, {
                    "type": "character",
                    "htmlEntity": "&#59728;",
                    "color": "#eefc08"
                }, {
                    "type": "character",
                    "htmlEntity": "&#59727;",
                    "color": "#eefc08"
                }, {
                    "type": "character",
                    "htmlEntity": "&#59726;",
                    "color": "#eefc08"
                },
                {
                    "type": "character",
                    "htmlEntity": "&#59725;",
                    "color": "#eefc08"
                },
                {
                    "type": "character",
                    "htmlEntity": "&#58931;",
                    "color": "#eefc08"
                },
                {
                    "type": "character",
                    "htmlEntity": "&#58923;",
                    "color": "#eefc08"
                },
                {
                    "type": "character",
                    "htmlEntity": "&#59710;",
                    "color": "#eefc08"
                }
            ],
            "elements": []
        }
    },

    {
        "type": "message",
        "content": {
            "id": 10,
            "origin": "peka2tv",
            "author": {"name": "fail0001", "id": "fail0001", "color": null},
            "text": "wwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwww",
            "timestamp": 1529402475110,
            "highlighted": false,
            "badges": [],
            "elements": []
        }
    },

    {
        "type": "message",
        "content": {
            "id": 11,
            "origin": "peka2tv",
            "author": {"name": "fail0001", "id": "fail0001", "color": null},
            "text": "highlighted message! one two three",
            "timestamp": 1529402475110,
            "highlighted": true,
            "badges": [],
            "elements": []
        }
    },

    {
        "type": "message",
        "content": {
            "id": 12,
            "timestamp": 1499789037199,
            "author": {
                "name": "theauthor",
                "id": "abc123",
                "color": null
            },
            "text": "link test {!0} 1223",
            "origin": "twitch",
            "highlighted": false,
            "elements": [
                {
                    "type": "link",
                    "domain": "google.com",
                    "fullUrl": "https://google.com",
                    "shortUrl": "google.com"
                }
            ],
            "badges": []
        }
    }

];
