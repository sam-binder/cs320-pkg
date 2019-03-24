// sets up the event listener on the collapsible menu toggler 
// (only applies to screens with small width viewport)
let collapsibleToggler = document.getElementById('navbar-toggler');
collapsibleToggler.addEventListener("click", function(){
    this.classList.toggle("open");
    let content = this.nextElementSibling;

    if(content.style.maxHeight) {
        content.style.maxHeight = null;
    }
    else {
        content.style.maxHeight = content.scrollHeight + "px";
    }
});