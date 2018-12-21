"use strict";

const failchat = {
    maxMessages: 50,
    iconsPath: "../_shared/icons/",
    removeMessageAnimationClass: null,
    origins: ["peka2tv", "twitch", "goodgame", "youtube", "cybergame"],
    deletedTextPlaceholder: "message deleted",
    hideMessages: false,
    hideMessagesAfter: 60,
    nativeClient: false
};

const templates = {
    message: new Template("message"),
    statusMessage: new Template("status-message"),
    rasterEmoticon: new Template("emoticon-raster"),
    vectorEmoticon: new Template("emoticon-vector"),
    link: new Template("link"),
    image: new Template("image"),
    originViewersBar: new Template("origin-viewers-bar")
};

const keyCode = {
    pageUp: 33,
    home: 36,
    up: 38
};

$(() => {
    awaitForTemplates().then(() => {
        initializeFailchat()
    });
});

async function awaitForTemplates() {
    for (const templateProperty in templates) {
        if (templates.hasOwnProperty(templateProperty)) {
            await templates[templateProperty].loaded;
        }
    }
}

function initializeFailchat() {
    failchat.nativeClient = (navigator.userAgent.search("failchat") >= 0);

    const activeMessages = [];
    let lastSystemMessageId = -1; //goes down
    let autoScroll = true;
    let showStatusMessages = true; // show if origin-status message received before client-configuration message

    // dom elements
    const bodyWrapper = $("#body-wrapper");
    const messageContainer = $("#message-container");
    const scroller = $(failchat.baronParams.scroller);
    const scrollBar = $(failchat.baronParams.bar);

    // viewers bar
    const viewersBar = $(".viewers-bar");
    const viewersCountItems = {};

    failchat.origins.forEach(origin => {
        const viewersBarHtml = templates.originViewersBar.render({origin: origin, iconsPath: failchat.iconsPath});
        $(".viewers-counters").append(viewersBarHtml);
        viewersCountItems[origin] = {
            bar: $("#" + origin + "-viewers-counter"),
            counter: $("#" + origin + "-viewers-count")
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


    // Dynamic styles
    const dynamicStyles = document.createElement("style");
    dynamicStyles.type = "text/css";
    document.head.appendChild(dynamicStyles);


    // Web socket
    const wsUrl = new URL("/chat", window.location.href);
    wsUrl.protocol = wsUrl.protocol.replace('http', 'ws');
    wsUrl.port = "10880"; // for debugging via idea

    const socket = new ReconnectingWebSocket(wsUrl);
    socket.maxReconnectInterval = 5000;
    failchat.socket = socket;

    socket.onopen = function() {
        const connectedMessage = {id: nextSystemMessageId(), "origin": "failchat", "status": "connected", "timestamp": Date.now()};
        handleStatusMessage(connectedMessage);

        socket.send(JSON.stringify({type: "client-configuration", content: {}}));
        socket.send(JSON.stringify({type: "viewers-count", content: {}}));
    };

    socket.onclose = function() {
        const disconnectedMessage = {id: nextSystemMessageId(), origin: "failchat", status: "disconnected", timestamp: Date.now()};
        handleStatusMessage(disconnectedMessage);
    };

    socket.onmessage = function() {
        handleMessage(JSON.parse(event.data));
    };

    function handleMessage(wsMessage) {
        const type = wsMessage.type;
        const content = wsMessage.content;

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
                resetHideMessageTasks();
                break;
        }

        if (!failchat.nativeClient) return;

        if (type === "viewers-count") {
            updateViewersValues(content);
        }
    }

    failchat.handleMessage = handleMessage;

    function handleChatMessage(chatMessage) {
        const elementsArray = chatMessage.elements;

        for (let i = 0; i < elementsArray.length; i++) {
            const element = elementsArray[i];
            let elementHtml;
            switch(element.type) {
                case "emoticon":
                    if (element.format === "vector") {
                        elementHtml = templates.vectorEmoticon.render(element);
                    } else {
                        elementHtml = templates.rasterEmoticon.render(element);
                    }
                    break;
                case "link":
                    elementHtml = templates.link.render(element);
                    break;
                case "image":
                    elementHtml = templates.image.render(element);
                    break;
            }

            chatMessage.text = chatMessage.text.replace("{!" + i + "}", elementHtml);
        }

        chatMessage.iconsPath = failchat.iconsPath;

        const messageHtml = templates.message.render(chatMessage);

        appendMessage(chatMessage, messageHtml)
    }

    function handleStatusMessage(statusMessage) {
        if (!showStatusMessages) return;

        statusMessage.iconsPath = failchat.iconsPath;
        const html = templates.statusMessage.render(statusMessage);
        appendMessage(statusMessage, html);
    }

    function handleClientConfigurationMessage(config) {
        const bodyZoomStyle = "body { zoom: " + config.zoomPercent + "%; }";

        let originBadgesStyle = "";
        if (config.showOriginBadges === false) {
            originBadgesStyle = ".message .origin-badge { display: none; }"
        }

        let userBadgesStyle = "";
        if (config.showUserBadges === false) {
            userBadgesStyle = ".message .user-badges { display: none; }"
        }

        dynamicStyles.innerHTML = bodyZoomStyle + originBadgesStyle + userBadgesStyle ;


        const statusMessageMode = config.statusMessageMode;
        if ((statusMessageMode === "everywhere") ||
            (failchat.nativeClient && statusMessageMode === "native_client")) {
            showStatusMessages = true;
        } else {
            showStatusMessages = false;
        }

        let bgHexColor;
        if (failchat.nativeClient) {
            bgHexColor = config.nativeClientBgColor;
        } else {
            bgHexColor = config.externalClientBgColor;
        }

        bodyWrapper.css("background-color", "rgba(" + hexToRgba(bgHexColor.substring(1)) + ")");


        failchat.hideMessages = config.hideMessages;
        failchat.hideMessagesAfter = config.hideMessagesAfter;

        // ---------- native client configuration ----------
        if (!failchat.nativeClient) return;

        // handle viewers bar configuration
        if (config.showViewersCount === true) {
            viewersBar.addClass("on");
        } else {
            viewersBar.removeClass("on");
        }

        const enabledOrigins = config.enabledOrigins;
        failchat.origins.forEach(origin => {
            if (!enabledOrigins.hasOwnProperty(origin)) return;
            if (enabledOrigins[origin] === true) {
                viewersCountItems[origin].bar.addClass("on");
            } else {
                viewersCountItems[origin].bar.removeClass("on");
            }
        });
    }

    function resetHideMessageTasks() {
        activeMessages.forEach(m => {
            cancelHideMessageTask(m);
            createHideMessageTask(m)
        });
    }

    function handleDeleteMessage(deleteMessage) {
        const message = $("#message-" + deleteMessage.messageId);
        const messageText = $("#message-" + deleteMessage.messageId + " .message-text");

        if (message === null || messageText === null) return;

        message.addClass("deleted-message");
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

    /** Append chat message or status message to message container. */
    function appendMessage(message, html) {
        activeMessages.push(message);
        messageContainer.append(html);

        if (autoScroll) {
            deleteOldMessages()
        }

        createHideMessageTask(message)
    }

    function hideAndDeleteMessage(message) {
        const messageElement = $("#message-" + message.id);

        if (failchat.removeMessageAnimationClass === null) {
            // delete without animation
            deleteMessage(message);
            return;
        }

        // delete with animation
        inactivateMessage(message);
        messageElement.on("animationend", null, null, () => {
            deleteMessageElement(message);
        });
        messageElement.addClass(failchat.removeMessageAnimationClass);
    }

    function inactivateMessage(message) {
        const index = activeMessages.indexOf(message);
        if (index > 0)
            activeMessages.splice(index, 1);

        cancelHideMessageTask(message);
    }

    function deleteMessageElement(message) {
        const element = $("#message-" + message.id);
        if (element != null)
            element.remove();
    }

    function deleteMessage(message) {
        inactivateMessage(message);
        deleteMessageElement(message);
    }

    function createHideMessageTask(message) {
        if (!failchat.hideMessages) return;

        const taskId = setTimeout(() => {
            hideAndDeleteMessage(message);
        }, failchat.hideMessagesAfter * 1000);
        message.hideTaskId = taskId;
    }

    function cancelHideMessageTask(message) {
        const taskId = message.hideTaskId;

        if (taskId instanceof Number) {
            clearTimeout(taskId)
        }
    }

    function deleteOldMessages() {
        if (activeMessages.length <= failchat.maxMessages) return;

        const messagesToDelete = activeMessages.length - failchat.maxMessages;
        const removedMessages = activeMessages.splice(0, messagesToDelete);

        removedMessages.forEach((m) => deleteMessage(m)) // delete w/o potential animation
    }

    function nextSystemMessageId() {
        return lastSystemMessageId--;
    }

    $("body,html").bind("keydown wheel mousewheel", e => {
        //checks for disabling autoscroll
        if (autoScroll) {
            if (e.originalEvent.deltaY < 0 ||
                (e.type === "keydown" && (e.keyCode === keyCode.home || e.keyCode === keyCode.pageUp || e.keyCode === keyCode.up))
            ) {
                autoScroll = false;
                scrollBar.css("visibility", "visible");
            }
            if (e.type === "mousewheel") { // for old browsers
                autoScroll = scroller.scrollTop() + scroller.height() >= Math.floor(messageContainer.height());
                if (!autoScroll) {
                    scrollBar.css("visibility", "visible");
                }
            }
        }
    });

    scroller.scroll(e => {
        //checks for enabling autoscroll
        if (!autoScroll) {
            autoScroll = scroller.scrollTop() + scroller.height() >= Math.floor(messageContainer.height());
            if (autoScroll) {
                scrollBar.css("visibility", "hidden");
            }
        }
    });
}

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

function toggleModButtonsDisplay(messageId) {
    const modButtons = $("#message-" + messageId + " .mod-buttons")[0];
    if (modButtons.style.display === "") { //default is display: none
        modButtons.style.display = "inline"
    } else {
        modButtons.style.display = ""
    }
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


/** Asynchronously initialized template. */
function Template(name) {
    this.jsrTemplate = null;

    const xhr = new XMLHttpRequest();
    xhr.open("GET", "../_shared/templates/" + name + ".tmpl.html");

    this.loaded = new Promise((resolve, reject) => {
        xhr.onload = () => {
            if (xhr.readyState !== 4) return;
            if (xhr.status === 200) {
                this.jsrTemplate = $.templates(xhr.responseText);
            } else {
                console.error("Failed to load template '" + name + "'. " + JSON.stringify(xhr));
            }
            resolve();
        };
        xhr.onerror = () => {
            console.error("Error during '" + name + "'template request. " + JSON.stringify(xhr));
            resolve();
        }
    });

    xhr.send();

    this.render = function (parameters) {
        if (this.jsrTemplate === null) {
            console.error("Can't render template '" + name + "', cause: not initialized");
            return null;
        }
        return this.jsrTemplate.render(parameters)
    }
}
