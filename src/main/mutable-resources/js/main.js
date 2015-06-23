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

    socket.onopen = function () {
        messageContainer.prepend("<p>Соединение открыто</p>");
    };
    socket.onclose = function () {
        messageContainer.prepend("<p>Соединение закрыто</p>");
    };
    socket.onmessage = function (event) {
        var wsm = JSON.parse(event.data);

        //smiles
        if (wsm.message.smiles != undefined) {
            var imgHtml;
            for (var i = 0; i < wsm.message.smiles.length; i++) {
                imgHtml = smileTemplate.render(wsm.message.smiles[i]);
                wsm.message.text = wsm.message.text.replace("{!" + wsm.message.smiles[i].objectNumber + "}", imgHtml);
            }
        }

        //links
        if (wsm.message.links != undefined) {
            var linkHtml;
            for (i = 0; i < wsm.message.links.length; i++) {
                linkHtml = linkTemplate.render(wsm.message.links[i]);
                wsm.message.text = wsm.message.text.replace("{!" + wsm.message.links[i].objectNumber + "}", linkHtml);
            }
        }

        var messageHtml = messageTemplate.render(wsm.message);
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
    };
});
