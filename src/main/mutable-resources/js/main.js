var failchat = {
    "maxMessages": 50
};

$(function () {
    var socket = new WebSocket("ws://localhost:8887");
    var messageCount = 0;
    var messageContainer = $("#message-container");
    var smileTemplate = $("#smile-template");
    var linkTemplate = $("#link-template");
    var messageTemplate = $("#message-template");
    var infoMessageTemplate = $("#info-message-template");
    var autoScroll = true;
    var autoScrolled = false;

    socket.onopen = function () {
        var connectedMessage = {"source": "failchat","text":"connected"};
        handleInfoMessage(connectedMessage);
        appendToMessageContainer(connectedMessage);
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

    function appendToMessageContainer(message) {
        messageCount++;
        messageContainer.append(message.text);
        if (messageCount > failchat.maxMessages && autoScroll) {
            while (messageCount > failchat.maxMessages) {
                messageContainer.find("> :first").remove();
                messageCount--;
            }
        }
        if (autoScroll) {
            if (message.smiles !== undefined && failchat.scrollHookSelector !== undefined) {
                scrollHook();
            } else {
                autoScrolled = true;
                $(document).scrollTop($(document).height());
            }
        }
        //console.log("messages: " + messageCount);
    }

    window.onscroll = function() {
        //console.log("scrolled");
        if (autoScrolled) {
            autoScrolled = false;
        }
        else {
            autoScroll = $(document).scrollTop() + window.innerHeight >= $(document).height();
            //console.log("autoScroll: " + autoScroll);
        }
    };

    // scroll when last smile in message loaded
    function scrollHook() {
        $(failchat.scrollHookSelector + ":last").imagesLoaded(function() {
            //console.log("last smile loaded");
            if (autoScroll) {
                autoScrolled = true;
                $(document).scrollTop($(document).height());
            }
        });
    }

});