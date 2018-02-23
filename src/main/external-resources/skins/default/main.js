"use strict";

var failchat = {
    maxMessages: 50,
    messageCount: 0,
    origins: ["peka2tv", "twitch", "goodgame", "youtube"],
    deletedTextPlaceholder: "message deleted"
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
    var showStatusMessages = true;

    //templates
    var smileTemplate = $("#smile-template");
    var vectorSmileTemplate = $("#vector-smile-template");
    var linkTemplate = $("#link-template");
    var imageTemplate = $("#image-template");
    var messageTemplate = $("#message-template");
    var statusMessageTemplate = $("#status-message-template");

    //viewers bar
    var viewersBar = $(".viewers-bar");
    var originViewersBarTemplate = $("#origin-viewers-bar-template");
    var viewersCountItems = {};

    failchat.origins.forEach(function (origin) {
        var viewersBarHtml = originViewersBarTemplate.render({"origin": origin});
        $(".viewers-origins").append(viewersBarHtml);
        viewersCountItems[origin] = {
            bar: $("#" + origin + "-origin"),
            counter: $("#" + origin + "-viewers")
        };

        //set default value
        viewersCountItems[origin].counter.text("?")
    });

    baron(failchat.baronParams);

    // auto scroll
    new ResizeSensor(messageContainer, function() {
        if (autoScroll) {
            scroller.scrollTop(messageContainer.height());
        }
    });


    socket.onopen = function () {
        var connectedMessage = {"origin": "failchat", "status": "connected", "timestamp": Date.now()};
        handleStatusMessage(connectedMessage);
        appendToMessageContainer(connectedMessage);

        socket.send(JSON.stringify({"type": "client-configuration", "content": {}}));
        socket.send(JSON.stringify({"type": "viewers-count", "content": {}}));
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
        var elementsArray = content.elements;

        for (var i = 0; i < elementsArray.length; i++) {
            var element = elementsArray[i];
            var elementHtml;
            switch(element.type) {
                case "emoticon":
                    if (element.format === "vector") {
                        elementHtml = vectorSmileTemplate.render(element);
                    } else {
                        elementHtml = smileTemplate.render(element);
                    }
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

    function handleClientConfigurationMessage(content) {
        var statusMessageMode = content.statusMessageMode;
        if ((statusMessageMode === "everywhere") ||
            (nativeClient && statusMessageMode === "native_client")) {
            showStatusMessages = true;
        } else {
            showStatusMessages = false;
        }


        if (!nativeClient) return;
        // handle viewers bar configuration

        if (content.showViewersCount === true) {
            viewersBar.addClass("viewers-bar-on");
        } else {
            viewersBar.removeClass("viewers-bar-on");
        }

        var enabledOrigins = content.enabledOrigins;
        failchat.origins.forEach(function (origin) {
            if (!enabledOrigins.hasOwnProperty(origin)) return;
            if (enabledOrigins[origin] === true) {
                viewersCountItems[origin].bar.addClass("viewers-origin-on");
            } else {
                viewersCountItems[origin].bar.removeClass("viewers-origin-on");
            }
        });
    }

    function handleDeleteMessage(content) {
        var messageText = $("#message-" + content.messageId + " .text");
        messageText.removeClass("highlighted");
        //todo add class for deleted message
        messageText.text(failchat.deletedTextPlaceholder);
    }

    function updateViewersValues(counters) {
        failchat.origins.forEach(function (origin) {
            if (!counters.hasOwnProperty(origin)) return;

            var count = counters[origin];
            var counter = viewersCountItems[origin].counter;
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
