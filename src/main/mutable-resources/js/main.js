var failchat = {
    "mode": 0,
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

    socket.onopen = function () {
        var openHtml = handleInfoMessage({"source": "failchat","text":"connected"});
        appendToMessageContainer(openHtml);
    };
    socket.onclose = function () {
        var openHtml = handleInfoMessage({"source": "failchat","text":"disconnected"});
        appendToMessageContainer(openHtml);
    };
    socket.onmessage = function (event) {
        var wsm = JSON.parse(event.data);
        var messageHtml = null;

        if (wsm.type === "message") {
            messageHtml = handleMessage(wsm.content);
        }
        else if (wsm.type === "info") {
            messageHtml = handleInfoMessage(wsm.content);
        }

        if (messageHtml !== null && messageHtml !== "") {
            appendToMessageContainer(messageHtml);
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

        return messageTemplate.render(message);
    }

    function handleInfoMessage(infoMessage) {
        return infoMessageTemplate.render(infoMessage);
    }

    function appendToMessageContainer(messageHtml) {
        messageCount++;
        if (failchat.mode == 0) {
            messageContainer.append(messageHtml);
            if (messageCount > failchat.maxMessages) {
                messageContainer.find("> :first").remove();
            }
            // TODO: scroll to bottom
        }
        else {
            messageContainer.prepend(messageHtml);
            if (messageCount > failchat.maxMessages) {
                messageContainer.find("> :last").remove();
            }
        }
    }
});