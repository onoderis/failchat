"use strict";

const failchat = {
    maxMessages: 50,
    messageCount: 0,
    iconsPath: "../_shared/icons/", //could be overrided in skin.html
    origins: ["peka2tv", "twitch", "goodgame", "youtube", "cybergame"],
    deletedTextPlaceholder: "message deleted"
};

$(() => {
    const socket = new ReconnectingWebSocket("ws://localhost:10880");
    socket.maxReconnectInterval = 5000;
    failchat.socket = socket;

    const bodyWrapper = $("#body-wrapper");
    const messageContainer = $("#message-container");
    const scroller = $(failchat.baronParams.scroller);
    const scrollBar = $(failchat.baronParams.bar);
    let autoScroll = true;
    const nativeClient = (navigator.userAgent.search("failchat") >= 0);
    // var nativeClient = true; //todo think about debugging
    let showStatusMessages = true; // show if origin-status message received before client-configuration message

    // viewers bar
    const viewersBar = $(".viewers-bar");
    const viewersCountItems = {};

    failchat.origins.forEach(origin => {
        const viewersBarHtml = templates.originViewersBar.render({origin: origin, iconsPath: failchat.iconsPath});
        $(".viewers-origins").append(viewersBarHtml);
        viewersCountItems[origin] = {
            bar: $("#" + origin + "-origin"),
            counter: $("#" + origin + "-viewers")
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


    socket.onopen = function() {
        const connectedMessage = {"origin": "failchat", "status": "connected", "timestamp": Date.now()};
        handleStatusMessage(connectedMessage);
        appendToMessageContainer(connectedMessage);

        socket.send(JSON.stringify({type: "client-configuration", content: {}}));
        socket.send(JSON.stringify({type: "viewers-count", content: {}}));
    };

    socket.onclose = function() {
        const disconnectedMessage = {origin: "failchat", status: "disconnected", timestamp: Date.now()};
        handleStatusMessage(disconnectedMessage);
        appendToMessageContainer(disconnectedMessage);
    };

    socket.onmessage = function() {
        const wsm = JSON.parse(event.data);
        const type = wsm.type;
        const content = wsm.content;

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
            appendToMessageContainer(wsm.content);
            return
        }

        if (!nativeClient) return;

        switch (type) {
            case "viewers-count":
                updateViewersValues(content);
                break;
        }
    };

    function handleChatMessage(content) {
        const elementsArray = content.elements;

        for (let i = 0; i < elementsArray.length; i++) {
            const element = elementsArray[i];
            let elementHtml;
            switch(element.type) {
                case "emoticon":
                    if (element.format === "vector") {
                        elementHtml = templates.vectorSmile.render(element);
                    } else {
                        elementHtml = templates.rasterSmile.render(element);
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
        content.textHtml = templates.statusMessage.render(content);
    }

    function handleClientConfigurationMessage(content) {
        const statusMessageMode = content.statusMessageMode;
        if ((statusMessageMode === "everywhere") ||
            (nativeClient && statusMessageMode === "native_client")) {
            showStatusMessages = true;
        } else {
            showStatusMessages = false;
        }

        let bgHexColor;
        if (nativeClient) {
            bgHexColor = content.nativeClientBgColor;
        } else {
            bgHexColor = content.externalClientBgColor;
        }

        bodyWrapper.css("background-color", "rgba(" + hexToRgba(bgHexColor.substring(1)) + ")");


        if (!nativeClient) return;
        // handle viewers bar configuration

        if (content.showViewersCount === true) {
            viewersBar.addClass("viewers-bar-on");
        } else {
            viewersBar.removeClass("viewers-bar-on");
        }

        const enabledOrigins = content.enabledOrigins;
        failchat.origins.forEach(origin => {
            if (!enabledOrigins.hasOwnProperty(origin)) return;
            if (enabledOrigins[origin] === true) {
                viewersCountItems[origin].bar.addClass("viewers-origin-on");
            } else {
                viewersCountItems[origin].bar.removeClass("viewers-origin-on");
            }
        });
    }

    function handleDeleteMessage(content) {
        const message = $("#message-" + content.messageId);
        message.addClass("deleted-message");

        const messageText = $("#message-" + content.messageId + " .text");
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

    function appendToMessageContainer(message) {
        failchat.messageCount++;
        if (failchat.messageCount > failchat.maxMessages && autoScroll) {
            $(failchat.messageSelector + ":lt(" + (failchat.messageCount - failchat.maxMessages) + ")").remove();
            failchat.messageCount = failchat.maxMessages;
        }
        messageContainer.append(message.textHtml);
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
            autoScroll = scroller.scrollTop() + scroller.height() >= messageContainer.height();
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
        '<p class="message" id="message-{{:id}}" message-id="{{:id}}" author-id="{{:author.id}}#{{:origin}}">' +
        '    <img class="icon" src="{{:iconsPath}}{{:origin}}.png">' +
        '    {{for badges}}' +
        '    <img class="badge" src="{{:url}}" {{if description !== null}}title="{{:description}}"{{/if}}>' +
        '    {{/for}}' +
        '    <span class="nick" title="{{time:timestamp}}" tabindex="0">{{:author.name}}: </span>' +
        '    <span class="mod-icons">' +
        '        <span title="delete" onclick="deleteMessage(this.parentNode.parentNode)">&#10060;</span>' +
        '        <span title="ignore" onclick="ignore(this.parentNode.parentNode)">&#128683;</span>' +
        '    </span>' +
        '    <span class="text {{if highlighted}}highlighted{{/if}}">{{:text}}</span>' +
        '</p>'
    ),

    rasterSmile: $.templates('<img class="smile" src="{{:url}}">'),
    vectorSmile: $.templates('<img class="smile-vector" src="{{:url}}">'),
    link: $.templates('<a href="{{:fullUrl}}">{{:domain}}</a>'),
    image: $.templates('<br><a href="{{:url}}"><img class="image" align="middle" src="{{:url}}"></a><br>'),

    statusMessage: $.templates('' +
        '<p class="message status-message">\n' +
        '    <img class="icon" src="{{:iconsPath}}{{:origin}}.png">\n' +
        '    <span class="status-origin" title="{{time:timestamp}}">{{:origin}} </span>\n' +
        '    <span class="status-text">{{:status}}</span>\n' +
        '</p>'
    ),

    originViewersBar: $.templates('' +
        '<span id="{{:origin}}-origin" class="viewers-origin">\n' +
        '    <img class="icon" src="{{:iconsPath}}{{:origin}}.png"> <span id="{{:origin}}-viewers"></span>\n' +
        '</span>'
    )
};
