"use strict";

const failchat = {
    maxMessages: 50,
    messageCount: 0,
    iconsPath: "../_shared/icons/", //could be overrided in skin.html
    origins: ["peka2tv", "twitch", "goodgame", "youtube", "cybergame"],
    deletedTextPlaceholder: "message deleted",
    nativeClient: false
};

$(() => {

    failchat.nativeClient = (navigator.userAgent.search("failchat") >= 0);

    const bodyWrapper = $("#body-wrapper");
    const messageContainer = $("#message-container");
    const scroller = $(failchat.baronParams.scroller);
    const scrollBar = $(failchat.baronParams.bar);
    let autoScroll = true;
    let showStatusMessages = true; // show if origin-status message received before client-configuration message

    // viewers bar
    const viewersBar = $(".viewers-bar");
    const viewersCountItems = {};

    failchat.origins.forEach(origin => {
        const viewersBarHtml = templates.originViewersBar.render({origin: origin, iconsPath: failchat.iconsPath});
        $(".viewers-counters").append(viewersBarHtml);
        viewersCountItems[origin] = {
            bar: $("#" + origin + "-viewers-counter"),
            counter: $("#" + origin + "-viewers-count")
        };

        //set default value
        viewersCountItems[origin].counter.text("?")
    });

    // scroll bar
    baron(failchat.baronParams);

    // auto scroll
    new ResizeSensor(messageContainer, () => {
        if (autoScroll) {
            scroller.scrollTop(messageContainer.height());
        }
    });


    // Dynamic styles
    const dynamicStyles = document.createElement("style");
    dynamicStyles.type = "text/css";
    // dynamicStyles.innerHTML = '.cssClass { color: #F00; }';
    document.head.appendChild(dynamicStyles);


    // Web socket
    const socket = new ReconnectingWebSocket("ws://localhost:10880");
    socket.maxReconnectInterval = 5000;
    failchat.socket = socket;

    socket.onopen = function() {
        const connectedMessage = {"origin": "failchat", "status": "connected", "timestamp": Date.now()};
        handleStatusMessage(connectedMessage);

        socket.send(JSON.stringify({type: "client-configuration", content: {}}));
        socket.send(JSON.stringify({type: "viewers-count", content: {}}));
    };

    socket.onclose = function() {
        const disconnectedMessage = {origin: "failchat", status: "disconnected", timestamp: Date.now()};
        handleStatusMessage(disconnectedMessage);
    };

    socket.onmessage = function() {
        handleMessage(JSON.parse(event.data));
    };

    function handleMessage(wsMessage) {
        const type = wsMessage.type;
        const content = wsMessage.content;

        switch (type) {
            case "message":
                handleChatMessage(content);
                break;
            case "origin-status":
                handleStatusMessage(content);
                break;
            case "delete-message":
                handleDeleteMessage(content);
                break;
            case "client-configuration":
                handleClientConfigurationMessage(content);
                break;
        }

        if (content.textHtml !== undefined) {
            appendToMessageContainer(content.textHtml);
            return
        }

        if (!failchat.nativeClient) return;

        switch (type) {
            case "viewers-count":
                updateViewersValues(content);
                break;
        }
    }

    failchat.handleMessage = handleMessage;

    function handleChatMessage(content) {
        const elementsArray = content.elements;

        for (let i = 0; i < elementsArray.length; i++) {
            const element = elementsArray[i];
            let elementHtml;
            switch(element.type) {
                case "emoticon":
                    if (element.format === "vector") {
                        elementHtml = templates.vectorEmoticon.render(element);
                    } else {
                        elementHtml = templates.rasterEmoticon.render(element);
                    }
                    break;
                case "link":
                    elementHtml = templates.link.render(element);
                    break;
                case "image":
                    elementHtml = templates.image.render(element);
                    break;
            }

            content.text = content.text.replace("{!" + i + "}", elementHtml);
        }

        content.iconsPath = failchat.iconsPath;

        content.textHtml = templates.message.render(content);
    }

    function handleStatusMessage(content) {
        if (!showStatusMessages) return;

        content.iconsPath = failchat.iconsPath;
        const html = templates.statusMessage.render(content);
        appendToMessageContainer(html);
    }

    function handleClientConfigurationMessage(content) {
        const bodyZoomStyle = "body { zoom: " + content.zoomPercent + "%; }";

        let originBadgesStyle = "";
        if (content.showOriginBadges === false) {
            originBadgesStyle = ".message .origin-badge { display: none; }"
        }

        let userBadgesStyle = "";
        if (content.showUserBadges === false) {
            userBadgesStyle = ".message .user-badges { display: none; }"
        }

        dynamicStyles.innerHTML = bodyZoomStyle + originBadgesStyle + userBadgesStyle ;


        const statusMessageMode = content.statusMessageMode;
        if ((statusMessageMode === "everywhere") ||
            (failchat.nativeClient && statusMessageMode === "native_client")) {
            showStatusMessages = true;
        } else {
            showStatusMessages = false;
        }

        let bgHexColor;
        if (failchat.nativeClient) {
            bgHexColor = content.nativeClientBgColor;
        } else {
            bgHexColor = content.externalClientBgColor;
        }

        bodyWrapper.css("background-color", "rgba(" + hexToRgba(bgHexColor.substring(1)) + ")");


        if (!failchat.nativeClient) return;
        // handle viewers bar configuration

        if (content.showViewersCount === true) {
            viewersBar.addClass("on");
        } else {
            viewersBar.removeClass("on");
        }

        const enabledOrigins = content.enabledOrigins;
        failchat.origins.forEach(origin => {
            if (!enabledOrigins.hasOwnProperty(origin)) return;
            if (enabledOrigins[origin] === true) {
                viewersCountItems[origin].bar.addClass("on");
            } else {
                viewersCountItems[origin].bar.removeClass("on");
            }
        });
    }

    function handleDeleteMessage(content) {
        const message = $("#message-" + content.messageId);
        const messageText = $("#message-" + content.messageId + " .message-text");

        if (message === null || messageText === null) return;

        message.addClass("deleted-message");
        messageText.removeClass("highlighted");
        messageText.text(failchat.deletedTextPlaceholder);
    }

    function updateViewersValues(counters) {
        failchat.origins.forEach(origin => {
            if (!counters.hasOwnProperty(origin)) return;

            const count = counters[origin];
            const counter = viewersCountItems[origin].counter;
            if (count === null) {
                counter.text("?");
            } else {
                counter.text(count);
            }
        });
    }

    function appendToMessageContainer(html) {
        failchat.messageCount++;
        if (failchat.messageCount > failchat.maxMessages && autoScroll) {
            $(failchat.messageSelector + ":lt(" + (failchat.messageCount - failchat.maxMessages) + ")").remove();
            failchat.messageCount = failchat.maxMessages;
        }
        messageContainer.append(html);
    }

    $("body,html").bind("keydown wheel mousewheel", e => {
        //checks for disabling autoscroll
        if (autoScroll) {
            if (e.originalEvent.deltaY < 0 ||
                (e.type === "keydown" && (e.keyCode === 38 || e.keyCode === 36 || e.keyCode === 33)) // 38-up;36-home;33-pageup
            ) {
                autoScroll = false;
                scrollBar.css("visibility", "visible");
            }
            if (e.type === "mousewheel") { // for old browsers
                autoScroll = scroller.scrollTop() + scroller.height() >= messageContainer.height();
                if (!autoScroll) {
                    scrollBar.css("visibility", "visible");
                }
            }
        }
    });

    scroller.scroll(e => {
        //checks for enabling autoscroll
        if (!autoScroll) {
            autoScroll = scroller.scrollTop() + scroller.height() >= Math.floor(messageContainer.height());
            if (autoScroll) {
                scrollBar.css("visibility", "hidden");
            }
        }
    });
});

// Add user to ignore list and delete message
function ignore(messageNode) {
    deleteMessage(messageNode);
    failchat.socket.send(JSON.stringify(
        {
            type: "ignore-author",
            content: {
                authorId: messageNode.getAttribute("author-id")
            }
        }
    ));
}

// Just delete message
function deleteMessage(messageNode) {
    failchat.socket.send(JSON.stringify(
        {
            type: "delete-message",
            content: {
                messageId: messageNode.getAttribute("message-id")
            }
        }
    ));
}

function hexToRgba(hex) {
    const int = parseInt(hex, 16);
    const r = (int >> 24) & 255;
    const g = (int >> 16) & 255;
    const b = (int >> 8) & 255;
    const a = (int & 255) / 255;

    return r + "," + g + "," + b + "," + a;
}

$.views.converters("time", val => {
    const d = new Date(val);
    const h = d.getHours().toString();
    let m = d.getMinutes().toString();
    let s = d.getSeconds().toString();
    if (m.length === 1) {
        m = "0" + m;
    }
    if (s.length === 1) {
        s = "0" + s;
    }
    return h + ":" + m + ":" +  s;
});

// noinspection HtmlUnknownAttribute
const templates = {
    message: $.templates(
        '<div class="message" id="message-{{:id}}" message-id="{{:id}}" author-id="{{:author.id}}#{{:origin}}">\n' +
        '    <div class="badges">\n' +
        '        <img class="origin-badge" src="{{:iconsPath}}{{:origin}}.png">\n' +
        '        <div class="user-badges">\n' +
        '        {{for badges}}\n' +
        '            {{if type === "image"}}\n' +
        '                <img class="{{if format === "raster"}}badge-raster{{else}}badge-vector{{/if}}"\n' +
        '                     src="{{:url}}" {{if description !== null}}title="{{:description}}"{{/if}}>\n' +
        '            {{else type === "character"}}\n' +
        '                <span class="badge-character" style="color: {{:color}}">{{:htmlEntity}}</span>\n' +
        '            {{/if}}\n' +
        '        {{/for}}\n' +
        '        </div>\n' +
        '    </div>\n' +
        '    <div class="message-content">\n' +
        '        <span class="nick" title="{{time:timestamp}}" tabindex="0">{{:author.name}}</span>\n' +
        '        <div class="mod-buttons">\n' +
        '            <span title="delete" onclick="deleteMessage(this.parentNode.parentNode)">&#10060;</span>\n' +
        '            <span title="ignore" onclick="ignore(this.parentNode.parentNode)">&#128683;</span>\n' +
        '        </div>\n' +
        '        <span class="message-text{{if highlighted}} highlighted{{/if}}">{{:text}}</span>\n' +
        '    </div>\n' +
        '</div>'
    ),

    rasterEmoticon: $.templates('<img class="emoticon-raster" src="{{:url}}" title="{{:code}}">'),
    vectorEmoticon: $.templates('<img class="emoticon-vector" src="{{:url}}" title="{{:code}}">'),
    link: $.templates('<a href="{{:fullUrl}}">{{:domain}}</a>'),
    image: $.templates('<div class="image-wrapper"><a href="{{:url}}"><img class="image" src="{{:url}}"></a></div>'),

    statusMessage: $.templates(
        '<div class="message status-message">\n' +
        '    <div class="badges">\n' +
        '        <img class="origin-badge" src="{{:iconsPath}}{{:origin}}.png">\n' +
        '    </div>\n' +
        '    <div class="message-content">\n' +
        '        <span class="origin-name" title="{{time:timestamp}}">{{:origin}} </span>\n' +
        '        <span class="status-text">{{:status}}</span>\n' +
        '    </div>\n' +
        '</div>'
    ),

    originViewersBar: $.templates(
        '<div id="{{:origin}}-viewers-counter" class="viewers-counter">' +
        '    <img class="origin-badge" src="{{:iconsPath}}{{:origin}}.png"><span id="{{:origin}}-viewers-count"></span>' +
        '</div>'
    )
};
