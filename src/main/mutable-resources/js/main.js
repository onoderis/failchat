var socket = new WebSocket("ws://localhost:8887");
var messageCount = 0;

socket.onopen = function () {
    $("#container").prepend("<p>Соединение открыто</p>");
};
socket.onclose = function () {
    $("#container").prepend("<p>Соединение закрыто</p>");
};
socket.onmessage = function (event) {
    var wsm = JSON.parse(event.data);

    //smiles
    if (wsm.message.smiles != undefined) {
        var imgHtml;
        for (var i = 0; i < wsm.message.smiles.length; i++) {
            imgHtml = $("#smileTemplate").render(wsm.message.smiles[i]);
            wsm.message.text = wsm.message.text.replace("{!" + wsm.message.smiles[i].objectNumber + "}", imgHtml);
        }
    }

    //links
    if (wsm.message.links != undefined) {
        var linkHtml;
        for (i = 0; i < wsm.message.links.length; i++) {
            linkHtml = $("#linkTemplate").render(wsm.message.links[i]);
            wsm.message.text = wsm.message.text.replace("{!" + wsm.message.links[i].objectNumber + "}", linkHtml);
        }
    }

    $("#container").prepend(
        $("#messageTemplate").render(wsm.message)
    );
    if (messageCount > 50) {
        $("#container").find("p:last").remove();
    }
    messageCount++;
};

String.prototype.insert = function (index, string) {
    if (index > 0)
        return this.substring(0, index) + string + this.substring(index, this.length);
    else
        return string + this;
};