function expandUp(li) {
  const parentId = li.attr("data-parent");
  if (parentId != "ROOT") {
    const span = $('#' + parentId);
    const parentLi = span.parents('li').first();
    span.expand(parentLi, false);
    expandUp(parentLi);
  }
}

function setActiveMenu() {
    $('li.on').each(function() {
      const onLi = $(this)
      expandUp(onLi);
      return false;
  });
}
