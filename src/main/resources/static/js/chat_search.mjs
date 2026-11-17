import { messageManager } from "./messages.mjs";
import { chatBar } from "./chat.mjs";

const password_form = document.getElementById("chat_password_form");
const overlay = document.getElementsByClassName("overlay")[0];
overlay.addEventListener('click', () => password_form.style.display = 'none');

let openChat = async (chat_name, password) => {
    let res = await messageManager.joinChat(chat_name, password);
    if (res.ok) {
        chatBar.addNewChat(chat_name);
        res = await messageManager.switchChat(chat_name);
        password_form.style.display = 'none';
        overlay.style.display = 'none';
    }
    return res;
}

class Search {
    constructor() {
        this.suggestion_conteiner = document.getElementById("search_suggestions_conteiner");
        this.suggestion_list = [];
        this.selection_idx = 0;
        this.latest = 0;

        this.searchbar = document.getElementById('search_input');

        this.searchbar.addEventListener('input', this.onInput);
        this.searchbar.addEventListener('keydown', this.onKeyDown);
        document.getElementById("search_enter_button").addEventListener('click', this.onSearchSubmit);

        this.pending_chat = null;
        password_form.addEventListener("submit", async (e) => {
            e.preventDefault();
            let res = await openChat(this.pending_chat, e.target.password.value);
            let pwd = document.getElementById("password");
            pwd.value = "";
            if (res.ok) {
                overlay.style.display = "none";
                password_form.style.display = "none";
            } else {
                pwd.classList.add('input_error_highlight');
                setTimeout(() => {
                    pwd.classList.remove('input_error_highlight');
                }, 4500);
            }
        });
    }

    async getSuggestions(querry) {
        let response = await fetch("/search?q=" + querry, {
            method: "GET",
        });

        if (response.ok) {
            let list = await response.json();
            return list;
        }

        console.log("search failed", response.status);
        return [];
    } 

    addSuggestion(name, isPrivate) {
        const div = document.createElement('div');
        div.className = 'search_suggestion';

        const nameLabel = document.createElement('label');
        nameLabel.textContent = name;
        const privateLabel = document.createElement('label');
        let text = isPrivate ? "Private" : "Public";
        privateLabel.textContent = `(${text})`;

        div.append(nameLabel, privateLabel);
        div.addEventListener('click', this.onSearchSubmit);

        this.suggestion_conteiner.append(div);
    }

    onInput = async (e) => {
        e.preventDefault();

        let id = ++this.latest;

        const querry = e.target.value;
        this.suggestion_list = await this.getSuggestions(querry);
        if (id != this.latest) return;

        this.suggestion_conteiner.replaceChildren();
        for (let suggestion of this.suggestion_list) {
            this.addSuggestion(suggestion.chatName, suggestion.isPrivate);
        }

        this.selection_idx = 0;
        this.moveSelection(0);
    }

    moveSelection(step) {
        let new_idx = this.selection_idx + step;

        if (this.suggestion_list.length == 0 || 
            new_idx < 0 || this.suggestion_list.length <= new_idx) { 
            return;
        }
        let curr = this.suggestion_conteiner.children[this.selection_idx];
        let next = this.suggestion_conteiner.children[new_idx];

        curr.className = "search_suggestion";
        next.className = "search_suggestion search_suggestion_selected";

        this.selection_idx = new_idx;
    }

    hideSuggestions() {
        this.suggestion_conteiner.replaceChildren();
        this.selection_idx = 0;
    }

    onSearchSubmit = () => {
        this.searchbar.value = "";
        if (this.suggestion_list.length == 0) return;
        this.pending_chat = this.suggestion_list[this.selection_idx].chatName;
        let isPrivate = this.suggestion_list[this.selection_idx].isPrivate;
        this.hideSuggestions();

        if (isPrivate) {
            overlay.style.display = "block";
            password_form.style.display = "block";
        } else {
            openChat(this.pending_chat, "");
        }
    }

    onKeyDown = (e) => {
        if (e.key === 'ArrowDown') {
            this.moveSelection(1);
        } 
        else if (e.key === 'ArrowUp') {
            this.moveSelection(-1);
        } 
        else if (e.key === 'Enter') {
            this.onSearchSubmit()
        }
        else if (e.key === 'Escape') {
            this.hideSuggestions();
        }
    }
}

let search = new Search();

