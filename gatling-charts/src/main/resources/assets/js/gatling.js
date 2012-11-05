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

	$.fn.sortable = function (target) {
		var table = this;

		this.find('thead .sortable').click( function () {
			var $this = $(this);

			if ($this.hasClass('sorted-down')) {
				var desc = false;
				var style = 'sorted-up';
			}
			else {
				var desc = true;
				var style = 'sorted-down';
			}

			$(target).sortTable($this.attr('id'), desc);

			table.find('thead .sortable').removeClass('sorted-up sorted-down');
			$this.addClass(style);

			return false;
		});

		return this;
	};

	$.fn.sortTable = function (col, desc) {
		function getValue(line) {
			var cell = $(line).find('.' + col);

			if (cell.hasClass('value'))
				var value = cell.text();
			else
				var value = cell.find('.value').text();

			return parseInt(value);
		}

		function sortLines (lines, group) {
			var sortedLines = lines.filter('.child-of-' + group).sort(function (a, b) {
				return desc ? getValue(b) - getValue(a): getValue(a) - getValue(b);
			}).toArray();

			var result = [];
			$.each(sortedLines, function (i, line) {
				result.push(line);
				result = result.concat(sortLines(lines, $(line).attr('id')));
			});

			return result;
		}

		this.find('tbody').append(sortLines(this.find('tbody tr').detach(), 'ROOT'));

		return this;
	};
})(jQuery);


