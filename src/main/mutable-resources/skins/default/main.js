var failchat = {
    "maxMessages": 50,
    "messageCount": 0
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
    //var nativeClient = true;

    //templates
    var smileTemplate = $("#smile-template");
    var linkTemplate = $("#link-template");
    var imageTemplate = $("#image-template");
    var messageTemplate = $("#message-template");
    var infoMessageTemplate = $("#info-message-template");

    //viewers bar
    var viewersBarEnabled = false;
    var viewersBar = $(".viewers-bar");
    var sc2tvViewersBar = $("#sc2tv-source");
    var twitchViewersBar = $("#twitch-source");
    var goodgameViewersBar = $("#goodgame-source");
    var cybergameViewersBar = $("#cybergame-source");
    var sc2tvViewersCount = $("#sc2tv-viewers");
    var twitchViewersCount = $("#twitch-viewers");
    var goodgameViwersCount = $("#goodgame-viewers");
    var cybergameViwersCount = $("#cybergame-viewers");


    baron(failchat.baronParams);

    //autoscroll
    new ResizeSensor(messageContainer, function() {
        if (autoScroll) {
            scroller.scrollTop(messageContainer.height());
        }
    });

    socket.onopen = function () {
        var connectedMessage = {"source": "failchat","text":"connected", "timestamp" : Date.now()};
        handleInfoMessage(connectedMessage);
        appendToMessageContainer(connectedMessage);
        if (nativeClient) {
            socket.send(JSON.stringify({type: "viewers"}));
        }
    };
    socket.onclose = function () {
        var disconnectedMessage =  {"source": "failchat","text":"disconnected", "timestamp" : Date.now()};
        handleInfoMessage(disconnectedMessage);
        appendToMessageContainer(disconnectedMessage);
    };
    socket.onmessage = function (event) {
        var wsm = JSON.parse(event.data);

        if (wsm.type === "message") {
            handleMessage(wsm.content);
        }
        else if (nativeClient && wsm.type === "viewers") {
            handleViewersMessage(wsm.content);
        }
        else if (wsm.type === "info") {
            handleInfoMessage(wsm.content);
        }
        else if (wsm.type === "mod") {
            handleModMessage(wsm.content);
        }

        if (wsm.content.text !== undefined) {
            appendToMessageContainer(wsm.content);
        }
    };

    function handleMessage(message) {
        //smiles
        if (message.smiles != undefined) {
            var smileHtml;
            for (var i = 0; i < message.smiles.length; i++) {
                smileHtml = smileTemplate.render(message.smiles[i]);
                message.text = message.text.replace("{!" + message.smiles[i].objectNumber + "}", smileHtml);
            }
        }

        //links
        if (message.links != undefined) {
            var linkHtml;
            for (i = 0; i < message.links.length; i++) {
                linkHtml = linkTemplate.render(message.links[i]);
                message.text = message.text.replace("{!" + message.links[i].objectNumber + "}", linkHtml);
            }
        }

        //images
        if (message.images != undefined) {
            var imgHtml;
            for (i = 0; i < message.images.length; i++) {
                imgHtml = imageTemplate.render(message.images[i]);
                message.text = message.text.replace("{!" + message.images[i].objectNumber + "}", imgHtml);
            }
        }

        message.text = messageTemplate.render(message);
    }

    function handleInfoMessage(infoMessage) {
        infoMessage.text =  infoMessageTemplate.render(infoMessage);
    }

    function handleViewersMessage(viewersMessage) {
        //update viewers count
        if (viewersBarEnabled && viewersMessage.show) {
            updateViewersValues(viewersMessage);
        }
        //enable viewers bar
        else if (!viewersBarEnabled && viewersMessage.show) {
            viewersBarEnabled = true;
            viewersBar.addClass("viewers-bar-on");
            if (viewersMessage.sc2tv != undefined) {
                sc2tvViewersBar.addClass("viewers-source-on");
            }
            if (viewersMessage.twitch != undefined) {
                twitchViewersBar.addClass("viewers-source-on");
            }
            if (viewersMessage.goodgame != undefined) {
                goodgameViewersBar.addClass("viewers-source-on");
            }
            if (viewersMessage.cybergame != undefined) {
                cybergameViewersBar.addClass("viewers-source-on");
            }
            updateViewersValues(viewersMessage);
            if (autoScroll) {
                scroller.scrollTop(messageContainer.height());
            }
        }
        //disable viewers bar
        else if (viewersBarEnabled && !viewersMessage.show) {
            viewersBarEnabled = false;
            viewersBar.removeClass("viewers-bar-on");
            sc2tvViewersBar.removeClass("viewers-source-on");
            twitchViewersBar.removeClass("viewers-source-on");
            goodgameViewersBar.removeClass("viewers-source-on");
            cybergameViewersBar.removeClass("viewers-source-on");
        }
    }

    function handleModMessage(modMessage) {
        var messageText = $("#message-" + modMessage.messageId + " .text");
        messageText.removeClass("highlighted");
        messageText.text("message deleted");
    }

    function updateViewersValues(viewersMessage) {
        if (viewersMessage.sc2tv != undefined) {
            sc2tvViewersCount.text(viewersMessage.sc2tv);
        }
        if (viewersMessage.twitch != undefined) {
            twitchViewersCount.text(viewersMessage.twitch);
        }
        if (viewersMessage.goodgame != undefined) {
            goodgameViwersCount.text(viewersMessage.goodgame);
        }
        if (viewersMessage.cybergame != undefined) {
            cybergameViwersCount.text(viewersMessage.cybergame);
        }
    }

    function appendToMessageContainer(message) {
        failchat.messageCount++;
        if (failchat.messageCount > failchat.maxMessages && autoScroll) {
            $(failchat.messageSelector + ":lt(" + (failchat.messageCount - failchat.maxMessages) + ")").remove();
            failchat.messageCount = failchat.maxMessages;
        }
        messageContainer.append(message.text);
    }

    $("body,html").bind("keydown wheel mousewheel", function(e){
        //checks for disabling autoscroll
        if (autoScroll) {
            if (e.originalEvent.deltaY < 0 ||
                (e.type == "keydown" && (e.keyCode == 38||e.keyCode == 36||e.keyCode == 33)) // 38-up;36-home;33-pageup
            ) {
                autoScroll = false;
                scrollBar.css("visibility", "visible");
            }
            if (e.type == "mousewheel") { // for old browsers
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

//add user to ignore list and delete message
function ignore(messageNode) {
    failchat.socket.send(JSON.stringify(
        {"type": "ignore", "content": {"user": messageNode.getAttribute("data-user"), "messageId": messageNode.getAttribute("id").slice(8)}}));
}

//just delete message
function deleteMessage(messageNode) {
    failchat.socket.send(JSON.stringify(
        {"type": "delete-message", "content": {"messageId": messageNode.getAttribute("id").slice(8)}}));
}

$.views.converters("time", function(val) {
    var d = new Date(val);
    var h = d.getHours().toString();
    var m = d.getMinutes().toString();
    var s = d.getSeconds().toString();
    if (s.length == 1) {
        s = "0" + s;
    }
    if (m.length == 1) {
        m = "0" + s;
    }
    return h + ":" + m + ":" +  s;
});