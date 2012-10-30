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


