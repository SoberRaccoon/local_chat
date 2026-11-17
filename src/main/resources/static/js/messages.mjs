
class MessageManager {
    constructor() {
        const proto = location.protocol === "https:" ? "wss" : "ws";
        this.endpoint = `${proto}://${location.host}/chat/message`;
        this.socket = new WebSocket(this.endpoint);

        this.socket.onopen = () => console.log("socket connected");

        this.selectedChat = '';
        this.socket.onmessage = (event) => {
            const msg = JSON.parse(event.data);
            if (msg.chatName == this.selectedChat) {
                this.addMessage(msg.message, msg.sender);
            }
        };
    
        this.messages_conteiner = document.getElementById("messages_conteiner");
        let form = document.getElementById("message_input_form");
        form.addEventListener("submit", (e) => {
            e.preventDefault();
            let message_input = e.target.message_input;
            this.sendMessage(message_input.value, this.selectedChat);
            message_input.value = "";
        });
    }

    switchChat = async (chat_name) => {
        this.selectedChat = chat_name;
        let chat_conteiner = document.getElementById("chat_conteiner");
        chat_conteiner.style.display = "flex";
        let chat = document.getElementById("messages_conteiner"); 
        chat.replaceChildren();
        this.selectedChat = chat_name;

        let msgs = await this.getMessages(chat_name);

        for (let message of msgs) {
            this.addMessage(message.content, message.sender);
        }
    }

    async joinChat(chatName, password) {
        return await fetch("/chat/join", {
            method: "POST",
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ chatName, password })
        });
    }

    async getMessages(chatName) {
        let messages = [];
        let response = await fetch("/chat/get?chatName=" + chatName, {
            method: "GET"
        });

        if (response.ok) {
            let json = await response.json();
            for (let m of json) {
                messages.push(m);
            }
        } else {
            console.log("search failed", response.status);
        }
        return messages; 
    }

    addMessage = (message, sender) => {
        let username = localStorage.getItem("username");
        let direction = sender === username ? "flex-end" : "flex-start";

        let msg = document.createElement("div");
        msg.className = "chat_message";
        msg.style.alignSelf = direction;

        let messageLabel = document.createElement("label");
        messageLabel.className = "chat_message_text";
        messageLabel.textContent = message;

        let senderLabel = document.createElement("label");
        senderLabel.className = "chat_message_sender";
        senderLabel.textContent = sender;

        msg.appendChild(messageLabel);
        msg.appendChild(senderLabel);

        this.messages_conteiner.appendChild(msg);
    } 

    sendMessage = (message, chat_name) => {
        let msg = {
            chat_name: chat_name,
            content: message
        }
        this.socket.send( JSON.stringify(msg));
    }
}

export const messageManager = new MessageManager();

