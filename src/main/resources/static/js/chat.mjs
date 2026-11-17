
// ----- Chat SideBar ----- 

import { messageManager } from "./messages.mjs"

class ChatBar {
    constructor() {
        this.side_bar = document.getElementById("side_bar");
        this.chat_list = document.getElementById("chat_list");
        this.name_list = []; 

        fetch("/chat_names", {
            method: "GET",
        }).then( async (response) => {
            if (response.ok) {
                this.name_list = await response.json();
                this.updateList()
            }
        });

        this.curr_item = null;
    }

    updateList = () => {
        this.chat_list.replaceChildren();
        for (let chat_name of this.name_list) {
            this.addChatToList(chat_name);
        }
    }

    addChatToList = (chat_name) => {
        let item = document.createElement("li");
        item.className = "chat_list_item";

        let label = document.createElement("label");
        label.textContent = chat_name;

        let button = document.createElement("button");
        button.className = "enter_button";
        button.textContent = "🗑";

        item.appendChild(label);
        item.appendChild(button);

        chat_list.appendChild(item);
    }

    switchSelection = (new_item, chat_name) => {
        if (this.curr_item != null) {
            this.curr_item.className = "chat_list_item";
        }
        this.curr_item = new_item;
        this.curr_item.className = "chat_list_item_hl";
        messageManager.switchChat(chat_name);
    }

    selectChat = (event) => {
        event.stopPropagation();
        const chat_list_item = event.target.closest('li');

        if (!chat_list_item || !chat_list.contains(chat_list_item)) return;

        const label = chat_list_item.querySelector('label');
        if (!label) return;
        let chat_name = label.textContent;

        const deleteButton = event.target.closest('button');
        if (deleteButton) {
            chat_list_item.remove();
            fetch("/leave?chat_name=" + chat_name, {
                method: "GET",
            });
        } else {
            this.switchSelection(chat_list_item, chat_name);
        }
    } 

    clearSelection = (event) => {
        if (this.curr_item == null) return; 
        this.curr_item.className = "chat_list_item";
        this.curr_item = null;

        let chat_conteiner = document.getElementById("chat_conteiner");
        chat_conteiner.style.display = "none";
    }

    addNewChat = (chat_name) => {
        if (this.name_list.includes(chat_name)) return;
        this.addChatToList(chat_name);
    }  
}

export const chatBar = new ChatBar(); 

chat_list.addEventListener('click', chatBar.selectChat);
side_bar.addEventListener("click", chatBar.clearSelection);


// ----- New Chat Form ----- 

let overlay = document.getElementsByClassName('overlay')[0] 
let new_chat_form = document.getElementById("create_chatroom_form");
let err_element = document.getElementById("create_chatroom_error");

let hideNewChatForm = async (e) => {
    overlay.style.display = "none";
    new_chat_form.style.display = "none";
    err_element.textContent = "";
}

overlay.addEventListener('click', hideNewChatForm);

document.getElementById('new_chat_btn').addEventListener('click', async (e) => {
    overlay.style.display = 'block'
    new_chat_form.style.display = 'flex'
})

document.getElementById('make_private_checkbox').addEventListener('change', async (e) => {
    let pwd = document.getElementById('chat_password')
    pwd.style.display = pwd.style.display == 'none' ? 'block' : 'none' 
    if (pwd.style.display == 'none') {
        pwd.textContent = "";
    }
})

let createChat = async (e) => {
    e.preventDefault();
    let chat_name = e.target.chat_name.value;
    let password = e.target.chat_password.value;
    e.target.chat_name.value = "";
    e.target.chat_password.value = "";

    let response = await fetch("/chat/create", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
            chatName: chat_name,
            password: password,
        })
    });

    if (!response.ok) {
        let err = await response.text();
        if (err == "NAME_IN_USE") {
            err_element.textContent = "chat name in use";
        } else if (err == "BAD_NAME") {
            err_element.textContent = "invalid name format";
        } else if (err == "BAD_PASSWORD") {
            err_element.textContent = "weak password";
        } else {
            err_element.textContent = "server error";
        }
    } else {
        hideNewChatForm();
        chatBar.addNewChat(chat_name);
    }
}

new_chat_form.addEventListener('submit', createChat);
