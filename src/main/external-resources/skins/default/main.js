"use strict";

var failchat = {
    "maxMessages": 50,
    "messageCount": 0,
    "origins": ["peka2tv", "twitch", "goodgame"],
    "deletedTextPlaceholder": "message deleted"
};

$(function () {
    var socket = new ReconnectingWebSocket("ws://localhost:10880");
    socket.maxReconnectInterval = 5000;
    failchat.socket = socket;
    var messageContainer = $("#message-container");
    var scroller = $(failchat.baronParams.scroller);
    var scrollBar = $(failchat.baronParams.bar);
    var autoScroll = true;
    // var nativeClient = (navigator.userAgent.search("failchat") >= 0);
    var nativeClient = true; //todo think about debugging

    //templates
    var smileTemplate = $("#smile-template");
    var linkTemplate = $("#link-template");
    var imageTemplate = $("#image-template");
    var messageTemplate = $("#message-template");
    var infoMessageTemplate = $("#info-message-template");

    //viewers bar
    var viewersBarEnabled = false;
    var viewersBar = $(".viewers-bar");
    var peka2tvViewersBar = $("#peka2tv-source");
    var twitchViewersBar = $("#twitch-source");
    var goodgameViewersBar = $("#goodgame-source");
    // var cybergameViewersBar = $("#cybergame-source");
    var peka2tvViewersCount = $("#peka2tv-viewers");
    var twitchViewersCount = $("#twitch-viewers");
    var goodgameViewersCount = $("#goodgame-viewers");
    // var cybergameViewersCount = $("#cybergame-viewers");


    baron(failchat.baronParams);

    //autoscroll
    new ResizeSensor(messageContainer, function() {
        if (autoScroll) {
            scroller.scrollTop(messageContainer.height());
        }
    });


    socket.onopen = function () {
        var connectedMessage = {"id": "-10", "source": "failchat", "textHtml": "connected", "timestamp": Date.now()};
        handleInfoMessage(connectedMessage);
        appendToMessageContainer(connectedMessage);
        if (nativeClient) {
            socket.send(JSON.stringify({"type": "enabled-origins", "content": {}}));
            socket.send(JSON.stringify({"type": "show-viewers-count", "content": {}}));
            socket.send(JSON.stringify({"type": "viewers-count", "content": {}}));
        }
    };

    socket.onclose = function () {
        var disconnectedMessage = {"id": "-10", "source": "failchat", "textHtml": "disconnected", "timestamp": Date.now()};
        handleInfoMessage(disconnectedMessage);
        appendToMessageContainer(disconnectedMessage);
    };

    socket.onmessage = function (event) {
        var wsm = JSON.parse(event.data);
        var type = wsm.type;
        var content = wsm.content;

        switch (type) {
            case "message":
                handleChatMessage(content);
                break;
            case "info":
                handleInfoMessage(content);
                break;
            case "delete-message":
                handleDeleteMessage(content);
                break;
        }

        if (content.textHtml !== undefined) {
            appendToMessageContainer(wsm.content);
            return
        }

        if (!nativeClient) return;

        switch (type) {
            case "enabled-origins":
                handleEnabledOriginsMessage(content);
                break;
            case "show-viewers-count":
                handleShowViewersCountMessage(content);
                break;
            case "viewers-count":
                updateViewersValues(content);
                break;
        }
    };

    function handleChatMessage(content) {
        var elementsArray = content.elements;

        for (var i = 0; i < elementsArray.length; i++) {
            var element = elementsArray[i];
            var elementHtml;
            switch(element.type) {
                case "emoticon":
                    elementHtml = smileTemplate.render(element);
                    break;
                case "link":
                    elementHtml = linkTemplate.render(element);
                    break;
                case "image":
                    elementHtml = imageTemplate.render(element);
                    break;
            }

            content.text = content.text.replace("{!" + i + "}", elementHtml);
        }

        content.textHtml = messageTemplate.render(content);
    }

    function handleInfoMessage(content) {
        content.textHtml = infoMessageTemplate.render(content);
    }

    function handleEnabledOriginsMessage(content) {
        // todo refactor
        var origins = content.origins;
        if (origins.indexOf("peka2tv") !== -1) {
            peka2tvViewersBar.addClass("viewers-source-on");
        } else {
            peka2tvViewersBar.removeClass("viewers-source-on");
        }

        if (origins.indexOf("twitch") !== -1) {
            twitchViewersBar.addClass("viewers-source-on");
        } else {
            twitchViewersBar.removeClass("viewers-source-on");
        }

        if (origins.indexOf("goodgame") !== -1) {
            goodgameViewersBar.addClass("viewers-source-on");
        } else {
            goodgameViewersBar.removeClass("viewers-source-on");
        }
    }

    function handleShowViewersCountMessage(content) {
        if (content.show === true) {
            viewersBar.addClass("viewers-bar-on");
        } else {
            viewersBar.removeClass("viewers-bar-on");
        }
    }

    function handleDeleteMessage(content) {
        var messageText = $("#message-" + content.messageId + " .text");
        messageText.removeClass("highlighted");
        messageText.text(failchat.deletedTextPlaceholder);
    }

    function updateViewersValues(counters) {
        //todo refactor
        var value;
        if (counters.peka2tv !== undefined) {
            value = counters.peka2tv;
            if (value === null) {
                value = "?";
            }
            peka2tvViewersCount.text(value);
        }
        if (counters.twitch !== undefined) {
            value = counters.twitch;
            if (value === null) {
                value = "?";
            }
            twitchViewersCount.text(value);
        }
        if (counters.goodgame !== undefined) {
            value = counters.goodgame;
            if (value === null) {
                value = "?";
            }
            goodgameViewersCount.text(value);
        }
    }

    function appendToMessageContainer(message) {
        failchat.messageCount++;
        if (failchat.messageCount > failchat.maxMessages && autoScroll) {
            $(failchat.messageSelector + ":lt(" + (failchat.messageCount - failchat.maxMessages) + ")").remove();
            failchat.messageCount = failchat.maxMessages;
        }
        messageContainer.append(message.textHtml);
    }

    function scrollIfRequired() {
        if (autoScroll) {
            scroller.scrollTop(messageContainer.height());
        }
    }

    $("body,html").bind("keydown wheel mousewheel", function(e){
        //checks for disabling autoscroll
        if (autoScroll) {
            if (e.originalEvent.deltaY < 0 ||
                (e.type === "keydown" && (e.keyCode === 38||e.keyCode === 36||e.keyCode === 33)) // 38-up;36-home;33-pageup
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

    scroller.scroll(function (e) {
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
    failchat.socket.send(JSON.stringify(
        {
            "type": "ignore-user",
            "content": {
                "user": messageNode.getAttribute("data-user"),
                "messageId": messageNode.getAttribute("id").slice(8)
            }
        }
    ));
}

// Just delete message
function deleteMessage(messageNode) {
    failchat.socket.send(JSON.stringify(
        {
            "type": "delete-message",
            "content": {
                "messageId": messageNode.getAttribute("id").slice(8)
            }
        }
    ));
}

$.views.converters("time", function(val) {
    var d = new Date(val);
    var h = d.getHours().toString();
    var m = d.getMinutes().toString();
    var s = d.getSeconds().toString();
    if (m.length === 1) {
        m = "0" + m;
    }
    if (s.length === 1) {
        s = "0" + s;
    }
    return h + ":" + m + ":" +  s;
});
