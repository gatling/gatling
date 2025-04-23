function isEllipsed(id) {
  const child = document.getElementById(id);
  const parent = document.getElementById("parent-" + id);
  const emptyData = parent.getAttribute("data-content") === "";
  const hasOverflow = child.clientWidth < child.scrollWidth;

  if (hasOverflow) {
    if (emptyData) {
      parent.setAttribute("data-content", child.innerHTML);
    }
  } else if (!emptyData) {
    parent.setAttribute("data-content", "");
  }
}
