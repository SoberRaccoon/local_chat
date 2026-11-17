

document.getElementById('logout_button').addEventListener('click', async (e) => {
    e.preventDefault();
    await fetch('/logout', {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        }
    });

    window.location.href = "/login";
    localStorage.setItem("username", "");
})

let username = localStorage.getItem("username");
document.getElementById("user_name_display").textContent = username;

