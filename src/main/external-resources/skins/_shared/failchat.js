"use strict";

const failchat = {
    maxMessages: 50,
    iconsPath: "../_shared/icons/",
    hideMessageAnimationClass: null,
    origins: ["peka2tv", "twitch", "goodgame", "youtube", "cybergame"],
    deletedTextPlaceholder: "message deleted",
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

    const activeMessages = new LinkedList(); // head - newest message
    const originsStatus = new OriginsStatus();

    let lastSystemMessageId = -1; //goes down
    let autoScroll = true;
    let showStatusMessages = true; // show if origin-status message received before client-configuration message
    let hideMessages = false;
    let hideMessagesAfter = 60;
    let showHiddenMessages = false;

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

    const paramParser = new URLSearchParams(window.location.search);
    const paramPort = paramParser.get("port");
    let port;
    if (paramPort !== null) {
        port = paramPort
    } else {
        port = "10880"
    }
    wsUrl.port = port;

    const socket = new ReconnectingWebSocket(wsUrl);
    socket.maxReconnectInterval = 5000;

    socket.onopen = function() {
        const connectedMessage = {id: nextSystemMessageId(), "origin": "failchat", "status": "connected", "timestamp": Date.now()};
        appendStatusMessage(connectedMessage);

        socket.send(JSON.stringify({type: "client-configuration", content: {}}));
        socket.send(JSON.stringify({type: "viewers-count", content: {}}));
        socket.send(JSON.stringify({type: "origins-status", content: {}}));
    };

    socket.onclose = function() {
        const disconnectedMessage = {id: nextSystemMessageId(), origin: "failchat", status: "disconnected", timestamp: Date.now()};
        appendStatusMessage(disconnectedMessage);
        originsStatus.reset()
    };

    socket.onmessage = function(event) {
        handleMessage(JSON.parse(event.data));
    };

    failchat.socket = socket;


    function handleMessage(wsMessage) {
        const type = wsMessage.type;
        const content = wsMessage.content;

        switch (type) {
            case "message":
                handleChatMessage(content);
                break;
            case "origins-status":
                handleOriginsStatusMessage(content);
                break;
            case "delete-message":
                handleDeleteMessage(content);
                break;
            case "client-configuration":
                handleClientConfigurationMessage(content);
                break;
            case "clear-chat":
                handleClearChatMessage();
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

    function appendStatusMessage(message) {
        message.iconsPath = failchat.iconsPath;
        const html = templates.statusMessage.render(message);
        appendMessage(message, html);
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


        let clientConfig;
        if (failchat.nativeClient) {
            clientConfig = config.nativeClient;
        } else {
            clientConfig = config.externalClient;
        }

        showStatusMessages = clientConfig.showStatusMessages;

        const backgroundColor = clientConfig.backgroundColor;
        bodyWrapper.css("background-color", "rgba(" + hexToRgba(backgroundColor.substring(1)) + ")");

        if (isHideMessagePropertiesChanged(clientConfig)) {
            hideMessages = clientConfig.hideMessages;
            hideMessagesAfter = clientConfig.hideMessagesAfter;
            resetHideMessageTasks();
        }

        // ---------- native client configuration ----------
        if (!failchat.nativeClient) return;

        // handle hiddenMessages property
        if (showHiddenMessages !== config.showHiddenMessages) {
            showHiddenMessages = config.showHiddenMessages;
            for (let [message] of activeMessages) {
                const messageElement = $(messageSelector(message));
                if (showHiddenMessages) {
                    messageElement.addClass("unhidden")
                } else {
                    messageElement.removeClass("unhidden")
                }
            }
        }

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
        for (let [message] of activeMessages) {
            cancelHideMessageTask(message);
            createHideMessageTaskIfRequired(message);
        }
    }

    function isHideMessagePropertiesChanged(clientConfig) {
        const isSameValues = (hideMessages === clientConfig.hideMessages) &&
            (hideMessagesAfter === clientConfig.hideMessagesAfter);
        return !isSameValues
    }

    function handleDeleteMessage(deleteMessage) {
        const selector = messageSelectorById(deleteMessage.messageId);
        const message = $(selector);
        const messageText = $(selector + " .message-text");

        if (message === null || messageText === null) return;

        message.addClass("deleted-message");
        messageText.removeClass("highlighted");
        messageText.text(failchat.deletedTextPlaceholder);
    }

    function handleClearChatMessage() {
        for (let [message] of activeMessages) {
            hideMessage(message);
        }
    }

    function handleOriginsStatusMessage(message) {
        failchat.origins.forEach(origin => {
            const oldStatus = originsStatus[origin];
            const newStatus = message[origin];

            if (newStatus === oldStatus) return;

            originsStatus[origin] = newStatus;
            const connectedMessage = {id: nextSystemMessageId(), "origin": origin, "status": newStatus, "timestamp": Date.now()};
            appendStatusMessage(connectedMessage);
        });
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
        message.hidden = false;
        activeMessages.unshift(message);
        messageContainer.append(html);

        if (showHiddenMessages) {
            $(messageSelector(message)).addClass("unhidden")
        }

        if (autoScroll) {
            deleteOldMessages()
        }

        createHideMessageTaskIfRequired(message)
    }

    function hideMessage(message) {
        const messageElement = $(messageSelector(message));

        if (failchat.hideMessageAnimationClass === null) {
            // hide without animation
            messageElement.addClass("hidden");
            message.hidden = true;
            return;
        }

        // hide with animation
        messageElement.addClass(failchat.hideMessageAnimationClass);

        messageElement.on("animationend", null, null, () => {
            messageElement.addClass("hidden");
            messageElement.removeClass(failchat.hideMessageAnimationClass);
            message.hidden = true;
        });
        messageElement.addClass(failchat.hideMessageAnimationClass);
    }

    function removeMessageElement(message) {
        const element = $(messageSelector(message));
        if (element != null)
            element.remove();
    }

    function createHideMessageTaskIfRequired(message) {
        if (!hideMessages) return;
        if (message.hidden) return;

        const taskId = setTimeout(() => {
            hideMessage(message);
        }, hideMessagesAfter * 1000);
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

        while (activeMessages.length >= failchat.maxMessages) {
            const removedMessage = activeMessages.pop();
            cancelHideMessageTask(removedMessage);
            removeMessageElement(removedMessage);
        }
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

function messageSelector(message) {
    return messageSelectorById(message.id);
}

function messageSelectorById(id) {
    return "#message-" + id;
}

const status = {
    connected: "connected",
    disconnected: "disconnected"
};

function OriginsStatus() {
    this.reset = function() {
        failchat.origins.forEach((origin) => {
            this[origin] = status.disconnected;
        });
    };

    this.reset()
}
