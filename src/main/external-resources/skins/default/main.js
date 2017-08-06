"use strict";

var failchat = {
    "maxMessages": 50,
    "messageCount": 0,
    "origins": ["peka2tv", "twitch", "goodgame", "youtube"],
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
    var nativeClient = (navigator.userAgent.search("failchat") >= 0);
    // var nativeClient = true; //todo think about debugging

    //templates
    var smileTemplate = $("#smile-template");
    var linkTemplate = $("#link-template");
    var imageTemplate = $("#image-template");
    var messageTemplate = $("#message-template");
    var statusMessageTemplate = $("#status-message-template");

    //viewers bar
    var viewersBar = $(".viewers-bar");
    var originViewersBarTemplate = $("#origin-viewers-bar-template");
    var viewersCountItems = {};
    for (var i = 0; i < failchat.origins.length; i++) {
        var origin = failchat.origins[i];
        var viewersBarHtml = originViewersBarTemplate.render({"origin": origin});
        $(".viewers-origins").append(viewersBarHtml);
        viewersCountItems[origin] = {
            "bar": $("#" + origin + "-origin"),
            "counter": $("#" + origin + "-viewers")
        };

        //set default value
        viewersCountItems[origin].counter.text("?")
    }


    baron(failchat.baronParams);

    //autoscroll
    new ResizeSensor(messageContainer, function() {
        if (autoScroll) {
            scroller.scrollTop(messageContainer.height());
        }
    });


    socket.onopen = function () {
        var connectedMessage = {"origin": "failchat", "status": "connected", "timestamp": Date.now()};
        handleStatusMessage(connectedMessage);
        appendToMessageContainer(connectedMessage);
        if (nativeClient) {
            socket.send(JSON.stringify({"type": "enabled-origins", "content": {}}));
            socket.send(JSON.stringify({"type": "show-viewers-count", "content": {}}));
            socket.send(JSON.stringify({"type": "viewers-count", "content": {}}));
        }
    };

    socket.onclose = function () {
        var disconnectedMessage = {"origin": "failchat", "status": "disconnected", "timestamp": Date.now()};
        handleStatusMessage(disconnectedMessage);
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
            case "origin-status":
                handleStatusMessage(content);
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

    function handleStatusMessage(content) {
        if (content.mode === "native_client" && !nativeClient) return;
        content.textHtml = statusMessageTemplate.render(content);
    }

    function handleEnabledOriginsMessage(content) {
        for (var origin in content) {
            if (!content.hasOwnProperty(origin)) continue;
            if (content[origin] === true) {
                viewersCountItems[origin].bar.addClass("viewers-origin-on");
            } else {
                viewersCountItems[origin].bar.removeClass("viewers-origin-on");
            }
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
        for (var origin in counters) {
            if (!counters.hasOwnProperty(origin)) continue;

            var count = counters[origin];
            if (count === null) {
                viewersCountItems[origin].counter.text("?");
                continue;
            }
            viewersCountItems[origin].counter.text(count);
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

    $("body,html").bind("keydown wheel mousewheel", function (e) {
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
    deleteMessage(messageNode);
    failchat.socket.send(JSON.stringify(
        {
            "type": "ignore-author",
            "content": {
                "authorId": messageNode.getAttribute("author-id")
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
                "messageId": messageNode.getAttribute("message-id")
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
