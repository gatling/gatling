function getItemLink(item){
	return item.pathFormatted + '.html';
}

function setDetailsLinkUrl(){
    $.each(stats.contents, function (name, data) {
        $('#details_link').attr('href', getItemLink(data));
        return false;
    });
}

var MENU_ITEM_MAX_LENGTH = 20;

function menuItem(item, level, parent, group) {
    if (group)
        var style = 'group';
    else
        var style = '';

    var maxNameSize = MENU_ITEM_MAX_LENGTH - level * 2;

    if (item.name.length > maxNameSize) {
        var title = "class='item nav-tooltip' data-toggle='popover' data-placement='right' data-container='body' data-content='" + item.name + "''";
        var truncatedLength = Math.max(3, maxNameSize - 1);
        var displayName = item.name.substr(0, truncatedLength) + '&hellip;';
    }
    else {
        var title = 'class="item"';
        var displayName = item.name;
    }

    if (parent) {
      var dataParent = ' data-parent="' + ((level == 0) ? 'ROOT' : ("menu-" + parent)) + '"';
    } else {
      var dataParent = '';
    }

    if (group)
        var expandButton = '<span id="menu-' + item.pathFormatted + '" style="margin-left: ' + (level * 10) + 'px;" class="expand-button">&nbsp;</span>';
    else
        var expandButton = '<span id="menu-' + item.pathFormatted + '" style="margin-left: ' + (level * 10) + 'px;" class="expand-button hidden">&nbsp;</span>';

    return '<li' + dataParent + ' class="withTooltip">' + expandButton + '<a href="' + getItemLink(item) + '"' + title + '>' + displayName + '</a></li>';
}

function menuItemsForGroup(group, level, parent) {
    var items = '';

    if (level > 0)
        items += menuItem(group, level - 1, parent, true);

    $.each(group.contents, function (contentName, content) {
        if (content.type == 'GROUP')
            items += menuItemsForGroup(content, level + 1, group.pathFormatted);
        else if (content.type == 'REQUEST')
            items += menuItem(content, level, group.pathFormatted);
    });

    return items;
}

function setDetailsMenu(){
    $('.nav ul').append(menuItemsForGroup(stats, 0));
    $('.nav').expandable();
    $('.nav-tooltip').popover({trigger:'hover'});
}

function setGlobalMenu(){
    $('.nav ul')
      .append('<li><a class="item" href="#ranges">Ranges</a></li>')
      .append('<li><a class="item" href="#stats">Stats</a></li>')
      .append('<li><a class="item" href="#active_users">Active Users</a></li>')
      .append('<li><a class="item" href="#requests">Requests / sec</a></li>')
      .append('<li><a class="item" href="#responses">Responses / sec</a></li>');
}

function getLink(link){
    var a = link.split('/');
    return (a.length<=1)? link : a[a.length-1];
}

function expandUp(li) {
  const parentId = li.attr("data-parent");
  if (parentId != "ROOT") {
    const span = $('#' + parentId);
    const parentLi = span.parents('li').first();
    span.expand(parentLi, false);
    expandUp(parentLi);
  }
}

function setActiveMenu(){
    $('.nav a').each(function() {
      const navA = $(this)
      if(!navA.hasClass('expand-button') && navA.attr('href') == getLink(window.location.pathname)) {
        const li = $(this).parents('li').first();
        li.addClass('on');
        expandUp(li);
        return false;
    }
  });
}
