/*
 * Copyright 2011-2012 eBusiness Information, Groupe Excilys (www.excilys.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
function setDetailsLinkUrl(){
	if (stats.groups != null)
		var list = stats.groups;
	else
		var list = stats.requests;

	$.each(list, function (name, data) {
		$('#details_link').attr('href', encodeURIComponent('req_' + data.pathFormatted + '.html'));
		return false;
	});
}

var MENU_ITEM_MAX_LENGTH = 50;

function menuItem(item, level, parent, group) {
	if (group)
		var style = 'group';
	else
		var style = '';

	if (item.name.length > MENU_ITEM_MAX_LENGTH) {
		var title = ' title="' + item.name + '"';
		var displayName = item.name.substr(0, MENU_ITEM_MAX_LENGTH) + '...';
	}
	else {
		var title = '';
		var displayName = item.name;
	}

	if (parent)
		var style = ' class="child-of-menu-' + parent + '"';
	else
		var style = '';

	if (group)
		var expandButton = '<a id="menu-' + item.pathFormatted + '" href="#" style="margin-left: ' + ((level - 1) * 10) + 'px;" class="expand-button"></a>';
	else
		var expandButton = '<a id="menu-' + item.pathFormatted + '" href="#" style="margin-left: ' + ((level - 1) * 10) + 'px;" class="expand-button hidden"></a>';

	return '<li' + style + '><div class="item">' + expandButton + '<a href="req_' + item.pathFormatted + '.html"' + title + '>' + displayName + '</a></div></li>';
}

function menuItemsForGroup(group, level, parent) {
	var items = '';

	if (level > 0)
		items += menuItem(group, level - 1, parent, true);

	if (group.groups != null) {
		$.each(group.groups, function (groupName, childGroup) {
			items += menuItemsForGroup(childGroup, level + 1, group.pathFormatted);
		});
	}

	if (group.requests != null) {
		$.each(group.requests, function (requestName, request) {
			items += menuItem(request, level, group.pathFormatted);
		});
	}

	return items;
}

function setDetailsMenu(){
    $('.nav ul').append(menuItemsForGroup(stats, 0));

	$('.nav').expandable();
}

function setGlobalMenu(){
    $('.nav ul').append('<li><div class="item"><a href="#active_sessions">Active Sessions</a></div></li> \
		<li><div class="item"><a href="#requests">Requests / sec</a></div></li> \
		<li><div class="item"><a href="#transactions">Transactions / sec</a></div></li>');
}

function getLink(link){
	var a = link.split('/');
	return (a.length<=1)? link : a[a.length-1];
}
 
function setActiveMenu(){
	$('.nav a').each(function(){
		if(!$(this).hasClass('expand-button') && $(this).attr('href') == getLink(window.location.pathname)){
			$(this).parents('li').addClass('on');
			return false;
		}
	});
}

(function ($) {
	$.fn.expandable = function () {
		this.find('.expand-button:not([class*=hidden])').addClass('collapse').click(function () {
			var $this = $(this);
			var id = $this.attr('id');

			if ($this.hasClass('expand'))
				$this.expand();
			else
				$this.collapse();

			return false;
		});

		this.find('.expand-button.hidden').click(function () { return false });

		return this;
	};

	$.fn.expand = function () {
		$('.child-of-' + this.attr('id')).toggle(true);

		return this.toggleClass('expand').toggleClass('collapse');
	};

	$.fn.collapse = function () {
        $.each($('.child-of-' + this.attr('id') + ' a.expand-button.collapse'), function (i, element) {
			$(element).collapse();
		});

		$('.child-of-' + this.attr('id')).toggle(false);

		return this.toggleClass('expand').toggleClass('collapse');
	};

	$.fn.sortable = function () {
		var table = this;
		this.find('thead .sortable').click( function () {
			table.sortTable($(this).attr('id'));

			return false;
		});

		return this;
	};

	$.fn.sortTable = function (col) {
		return this.find('tbody').append(this.find('tbody tr').remove().sortLines('ROOT'));

		return this;
	}

	$.fn.sortLines = function (group) {};
})(jQuery);


