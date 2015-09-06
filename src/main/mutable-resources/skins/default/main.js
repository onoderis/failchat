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
    var messageTemplate = $("#message-template");
    var infoMessageTemplate = $("#info-message-template");

    //viewers bar
    var viewersBarEnabled = false;
    var viewersBar = $(".viewers-bar");
    var twitchViewersBar = $("#twitch-source");
    var goodgameViewersBar = $("#goodgame-source");
    var cybergameViewersBar = $("#cybergame-source");
    var twitchViewrsCount = $("#twitch-viewers");
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
        var connectedMessage = {"source": "failchat","text":"connected"};
        handleInfoMessage(connectedMessage);
        appendToMessageContainer(connectedMessage);
        if (nativeClient) {
            socket.send(JSON.stringify({type: "viewers"}));
        }
    };
    socket.onclose = function () {
        var disconnectedMessage =  {"source": "failchat","text":"disconnected"};
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

        if (wsm.content.text !== undefined) {
            appendToMessageContainer(wsm.content);
        }
    };

    function handleMessage(message) {
        //smiles
        if (message.smiles != undefined) {
            var imgHtml;
            for (var i = 0; i < message.smiles.length; i++) {
                imgHtml = smileTemplate.render(message.smiles[i]);
                message.text = message.text.replace("{!" + message.smiles[i].objectNumber + "}", imgHtml);
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
        }
        //disable viewers bar
        else if (viewersBarEnabled && !viewersMessage.show) {
            viewersBarEnabled = false;
            viewersBar.removeClass("viewers-bar-on");
            twitchViewersBar.removeClass("viewers-source-on");
            goodgameViewersBar.removeClass("viewers-source-on");
            cybergameViewersBar.removeClass("viewers-source-on");
        }
    }

    function updateViewersValues(viewersMessage) {
        if (viewersMessage.twitch != undefined) {
            twitchViewrsCount.text(viewersMessage.twitch);
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

//add user to ignore list
function ignore(elem) {
    failchat.socket.send(JSON.stringify({"type": "ignore", "user": elem.getAttribute("data-user")}));
    elem.parentNode.remove();
    failchat.messageCount--;
}