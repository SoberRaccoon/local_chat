let is_sign_up = false;
document.getElementById('signup-link').addEventListener('click', (e) => {
    e.preventDefault();
    const link = e.currentTarget;
    let btn = document.getElementById('login_button');

    if (is_sign_up) {
        btn.textContent = 'Log in';
        link.textContent = 'Sign up';
    } else {
        btn.textContent = 'Sign up';
        link.textContent = 'Log in';
    }
    is_sign_up = !is_sign_up;
})

const err_element = document.getElementById("login_error");
document.getElementById("login-form").addEventListener("submit", async (event) => {
    event.preventDefault();

    const form = event.target;
    const username = form.username.value;
    const password = form.password.value;

    let response;
    if (is_sign_up) {
        response = await fetch("/register", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ username, password })
        });
        if (!response.ok) {
            let err = await response.text();
            console.log(err);
            if (err == "BAD_USERNAME") {
                err_element.textContent = "username must be alphanumeric";
                return;
            } if (err == "USERNAME_IN_USE"){
                err_element.textContent = "username already in use";
                return;
            } if (err == "BAD_PASSWORD") {
                err_element.innerHTML = "password must be alphanumeric<br/>" +
                    "and longer then 4 characters";
                return;
            } else {
                err_element.textContent = "server error";
                return;
            } 
        }
    }

    response = await fetch("/login", {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        body: new URLSearchParams({
            username,
            password,
            "remember-me": "true"
        })
    });

    if (response.ok) {
        window.location.href = "/";
        localStorage.setItem('username', username);
        return
    }

    let err = await response.text();
    if (err == "BAD_USERNAME") {
        err_element.textContent = "username doesn't exist";
    } else if (err == "BAD_PASSWORD") {
        err_element.textContent = "password is incorrect";
    } else {
        err_element.textContent = "server error";
    } 
});
