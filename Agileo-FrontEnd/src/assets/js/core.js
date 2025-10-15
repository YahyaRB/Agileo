/* core.js – Angular-friendly (delegated + lazy init) */
(function ($, window, document) {
  "use strict";

  /** Constant div card */
  const DIV_CARD = 'div.card';

  /* --------------------------
   * Helpers
   * -------------------------- */

  // Execute once per element by marking it
  function once($el, key = 'inited') {
    if ($el.data(key)) return false;
    $el.data(key, true);
    return true;
  }

  // Observe DOM and run init when selector appears (runs once unless multi=true)
  function observeAndInit(selector, init, opts = { multi: false }) {
    const tryInit = () => {
      const $targets = $(selector);
      if (!$targets.length) return;

      $targets.each(function () {
        const $t = $(this);
        if (opts.multi || once($t, `__init_${selector}`)) init($t);
      });
    };

    // Try immediately (for SSR / static cases)
    tryInit();

    // Watch DOM mutations (Angular renders later)
    const mo = new MutationObserver(tryInit);
    mo.observe(document.documentElement, { childList: true, subtree: true });
  }

  // Hex to rgba helper used by sparkline highlight
  function hexToRgba(hex, alpha) {
    // allow 'indigo' etc. (fallback: return as-is)
    if (!/^#([A-Fa-f0-9]{3}){1,2}$/.test(hex)) return hex;
    let c = hex.substring(1).split('');
    if (c.length === 3) c = [c[0], c[0], c[1], c[1], c[2], c[2]];
    c = '0x' + c.join('');
    // eslint-disable-next-line no-bitwise
    const r = (c >> 16) & 255, g = (c >> 8) & 255, b = c & 255;
    return `rgba(${r},${g},${b},${alpha})`;
  }

  /* --------------------------
   * Global quick tasks
   * -------------------------- */

  setTimeout(function () {
    $('.page-loader-wrapper').fadeOut();
  }, 50);

  // Prevent dropdown menu close on inside click
  $(document).on('click', '.dropdown-menu', function (e) {
    e.stopPropagation();
  });

  /* --------------------------
   * Tooltips / Popovers (delegated)
   * -------------------------- */

  // Bootstrap delegated init so elements added later work
  if ($.fn.tooltip) {
    $('body').tooltip({
      selector: '[data-toggle="tooltip"]'
    });
  }
  if ($.fn.popover) {
    $('body').popover({
      selector: '[data-toggle="popover"]',
      html: true
    });
  }

  /* --------------------------
   * Card actions (delegated)
   * -------------------------- */

  $(document).on('click', '[data-toggle="card-remove"]', function (e) {
    const $card = $(this).closest(DIV_CARD);
    $card.remove();
    e.preventDefault();
    return false;
  });

  $(document).on('click', '[data-toggle="card-collapse"]', function (e) {
    const $card = $(this).closest(DIV_CARD);
    $card.toggleClass('card-collapsed');
    e.preventDefault();
    return false;
  });

  $(document).on('click', '[data-toggle="card-fullscreen"]', function (e) {
    const $card = $(this).closest(DIV_CARD);
    $card.toggleClass('card-fullscreen').removeClass('card-collapsed');
    e.preventDefault();
    return false;
  });

  /* --------------------------
   * Sparkline (lazy)
   * -------------------------- */
  if (window.require) {
    observeAndInit('[data-sparkline]', function ($chart) {
      require(['sparkline'], function () {
        if (!$chart || !$chart.length) return;
        const data = JSON.parse($chart.attr('data-sparkline') || '[]');
        const color = $chart.attr('data-sparkline-color') || '#666';
        $chart.sparkline(data, {
          type: $chart.attr('data-sparkline-type'),
          height: '100%',
          barColor: color,
          lineColor: color,
          fillColor: 'transparent',
          spotColor: color,
          spotRadius: 0,
          lineWidth: 2,
          highlightColor: hexToRgba(color, .6),
          highlightLineColor: '#666',
          defaultPixelsPerValue: 5
        });
      });
    }, { multi: true });
  }

  // block-header bar chart small examples (if present in HTML)
  observeAndInit('.bh_income', function ($el) {
    $el.sparkline('html', {
      type: 'bar',
      height: '30px',
      barColor: '#6435c9',
      barWidth: 5
    });
  });
  observeAndInit('.bh_traffic', function ($el) {
    $el.sparkline('html', {
      type: 'bar',
      height: '30px',
      barColor: '#e03997',
      barWidth: 5
    });
  });

  /* --------------------------
   * Circle Progress (lazy)
   * -------------------------- */
  observeAndInit('.chart-circle', function ($el) {
    if (!$.fn.circleProgress) return;
    $el.circleProgress({
      fill: { color: 'indigo' },
      size: $el.height(),
      startAngle: -Math.PI / 4 * 2,
      emptyFill: '#F4F4F4',
      lineCap: 'round'
    });
  }, { multi: true });

  /* --------------------------
   * Responsive tweaks
   * -------------------------- */
  function alterClass() {
    $('body').addClass('close_rightbar');
  }
  $(window).on('resize', alterClass);
  alterClass();

  // Right panel tab toggle (delegated)
  $(document).on('click', 'a.right_tab', function (e) {
    e.preventDefault();
    $('body').toggleClass('right_tb_toggle');
  });

  // Skin chooser (delegated)
  $(document).on('click', '.choose-skin li', function () {
    var $body = $('body');
    var $this = $(this);
    var existTheme = $('.choose-skin li.active').data('theme');
    $('.choose-skin li').removeClass('active');
    $body.removeClass('theme-' + existTheme);
    $this.addClass('active');
    $body.addClass('theme-' + $this.data('theme'));
  });

  /* --------------------------
   * Table filter (delegated)
   * -------------------------- */
  $(document).on('click', '.star', function () {
    $(this).toggleClass('star-checked');
  });

  $(document).on('click', '.ckbox label', function () {
    $(this).parents('tr').toggleClass('selected');
  });

  $(document).on('click', '.btn-filter', function () {
    var $target = $(this).data('target');
    if ($target !== 'all') {
      $('.table tr').hide();
      $('.table tr[data-status="' + $target + '"]').fadeIn('slow');
    } else {
      $('.table tr').hide().fadeIn('slow');
    }
  });

  /* --------------------------
   * Sidebar / Menus (delegated + lazy init)
   * -------------------------- */

  // metisMenu: init when .sidebar-nav appears (once)
  (function initMetisMenuLazy() {
    let inited = false;
    const tryInit = () => {
      if (inited) return;
      const $nav = $('.sidebar-nav');
      if ($nav.length && $.fn.metisMenu) {
        $nav.metisMenu();
        inited = true;
      }
    };
    tryInit();
    const mo = new MutationObserver(tryInit);
    mo.observe(document.documentElement, { childList: true, subtree: true });
  })();

  // Menu toggle (body offcanvas)
  $(document).on('click', '.menu_toggle', function (e) {
    e.preventDefault();
    $('body').toggleClass('offcanvas-active');
  });

  // Chat sidebar toggle
  $(document).on('click', '.chat_list_btn', function () {
    $('.chat_list').toggleClass('open');
  });

  // Grid vs list option in sidebar
  $(document).on('click', '.menu_option', function () {
    $('.metismenu').toggleClass('grid');
    $('.menu_option').toggleClass('active');
  });

  // User dropdown
  $(document).on('click', '.user_btn', function () {
    $('.user_div').toggleClass('open');
  });

  // Right tab user chat open/close
  $(document).on('click', '.right_chat li a, .user_chatbody .chat_close', function (e) {
    e.preventDefault();
    $('.user_chatbody').toggleClass('open');
  });



  // Theme options panel
  $(document).on('click', '.theme_btn', function (e) {
    e.preventDefault();
    $('.theme_div').toggleClass('open');
  });

  // Click on page closes panels
  $(document).on('click', '.page', function () {
    $('.theme_div, .right_sidebar').removeClass('open');
    $('.user_div').removeClass('open');
  });

  // Light/Dark switch for body theme
  $(document).on('click', '.theme_switch', function () {
    $('body').toggleClass('theme-dark');
  });

  /* --------------------------
   * Font & Icon settings (delegated)
   * -------------------------- */

  $(document).on('click', '.arrow_option input:radio', function () {
    const others = $("[name='" + this.name + "']").map(function () {
      return this.value;
    }).get().join(" ");
    $('.metismenu .has-arrow').removeClass(others).addClass(this.value);
  });

  $(document).on('click', '.list_option input:radio', function () {
    const others = $("[name='" + this.name + "']").map(function () {
      return this.value;
    }).get().join(" ");
    $('.metismenu li .collapse a').removeClass(others).addClass(this.value);
  });

  $(document).on('click', '.font_setting input:radio', function () {
    const others = $("[name='" + this.name + "']").map(function () {
      return this.value;
    }).get().join(" ");
    $('body').removeClass(others).addClass(this.value);
  });

  /* --------------------------
   * Switch settings (delegated)
   * -------------------------- */
  $(document).on('change', '.setting_switch .btn-darkmode', function () {
    $(this).prop('checked') ? $('body').addClass('dark-mode') : $('body').removeClass('dark-mode');
  });

  $(document).on('change', '.setting_switch .btn-fixnavbar', function () {
    $(this).prop('checked') ? $('#page_top').addClass('sticky-top') : $('#page_top').removeClass('sticky-top');
  });

  $(document).on('change', '.setting_switch .btn-iconcolor', function () {
    $(this).prop('checked') ? $('body').addClass('iconcolor') : $('body').removeClass('iconcolor');
  });

  $(document).on('change', '.setting_switch .btn-gradient', function () {
    $(this).prop('checked') ? $('body').addClass('gradient') : $('body').removeClass('gradient');
  });

  $(document).on('change', '.setting_switch .btn-sidebar', function () {
    $(this).prop('checked') ? $('body').addClass('sidebar_dark') : $('body').removeClass('sidebar_dark');
  });

  $(document).on('change', '.setting_switch .btn-min_sidebar', function () {
    $(this).prop('checked') ? $('#header_top').addClass('dark') : $('#header_top').removeClass('dark');
  });

  $(document).on('change', '.setting_switch .btn-pageheader', function () {
    $(this).prop('checked') ? $('#page_top').addClass('top_dark') : $('#page_top').removeClass('top_dark');
  });

  $(document).on('change', '.setting_switch .btn-boxshadow', function () {
    $(this).prop('checked') ? $('.card, .btn, .progress').addClass('box_shadow') : $('.card, .btn, .progress').removeClass('box_shadow');
  });

  $(document).on('change', '.setting_switch .btn-rtl', function () {
    $(this).prop('checked') ? $('body').addClass('rtl') : $('body').removeClass('rtl');
  });

  $(document).on('change', '.setting_switch .btn-boxlayout', function () {
    $(this).prop('checked') ? $('body').addClass('boxlayout') : $('body').removeClass('boxlayout');
  });

  /* --------------------------
   * Search list (lazy)
   * -------------------------- */
  observeAndInit('#users', function () {
    if (!window.List) return;
    // eslint-disable-next-line no-new
    new List('users', { valueNames: ['name', 'born'] });
  });

  /* --------------------------
   * Theme colors (unchanged, global)
   * -------------------------- */
  window.anchor = {
    colors: {
      'theme1-one': '#6435c9',
      'theme1-two': '#f66d9b',

      'blue': '#467fcf',
      'blue-darkest': '#0e1929',
      'blue-darker': '#1c3353',
      'blue-dark': '#3866a6',
      'blue-light': '#7ea5dd',
      'blue-lighter': '#c8d9f1',
      'blue-lightest': '#edf2fa',
      'azure': '#45aaf2',
      'azure-darkest': '#0e2230',
      'azure-darker': '#1c4461',
      'azure-dark': '#3788c2',
      'azure-light': '#7dc4f6',
      'azure-lighter': '#c7e6fb',
      'azure-lightest': '#ecf7fe',

      'indigo': '#6435c9',
      'indigo-darkest': '#3e0ca9',
      'indigo-darker': '#5322bb',
      'indigo-dark': '#5929c1',
      'indigo-light': '#7d53d6',
      'indigo-lighter': '#9773e4',
      'indigo-lightest': '#a28ad6',

      'purple': '#a55eea',
      'purple-darkest': '#21132f',
      'purple-darker': '#42265e',
      'purple-dark': '#844bbb',
      'purple-light': '#c08ef0',
      'purple-lighter': '#e4cff9',
      'purple-lightest': '#f6effd',

      'pink': '#f66d9b',
      'pink-darkest': '#31161f',
      'pink-darker': '#622c3e',
      'pink-dark': '#c5577c',
      'pink-light': '#f999b9',
      'pink-lighter': '#fcd3e1',
      'pink-lightest': '#fef0f5',

      'red': '#e74c3c',
      'red-darkest': '#2e0f0c',
      'red-darker': '#5c1e18',
      'red-dark': '#b93d30',
      'red-light': '#ee8277',
      'red-lighter': '#f8c9c5',
      'red-lightest': '#fdedec',

      'orange': '#fd9644',
      'orange-darkest': '#331e0e',
      'orange-darker': '#653c1b',
      'orange-dark': '#ca7836',
      'orange-light': '#feb67c',
      'orange-lighter': '#fee0c7',
      'orange-lightest': '#fff5ec',

      'yellow': '#f1c40f',
      'yellow-darkest': '#302703',
      'yellow-darker': '#604e06',
      'yellow-dark': '#c19d0c',
      'yellow-light': '#f5d657',
      'yellow-lighter': '#fbedb7',
      'yellow-lightest': '#fef9e7',

      'lime': '#7bd235',
      'lime-darkest': '#192a0b',
      'lime-darker': '#315415',
      'lime-dark': '#62a82a',
      'lime-light': '#a3e072',
      'lime-lighter': '#d7f2c2',
      'lime-lightest': '#f2fbeb',

      'green': '#5eba00',
      'green-darkest': '#132500',
      'green-darker': '#264a00',
      'green-dark': '#4b9500',
      'green-light': '#8ecf4d',
      'green-lighter': '#cfeab3',
      'green-lightest': '#eff8e6',

      'teal': '#2bcbba',
      'teal-darkest': '#092925',
      'teal-darker': '#11514a',
      'teal-dark': '#22a295',
      'teal-light': '#6bdbcf',
      'teal-lighter': '#bfefea',
      'teal-lightest': '#eafaf8',

      'cyan': '#17a2b8',
      'cyan-darkest': '#052025',
      'cyan-darker': '#09414a',
      'cyan-dark': '#128293',
      'cyan-light': '#5dbecd',
      'cyan-lighter': '#b9e3ea',
      'cyan-lightest': '#e8f6f8',

      'gray': '#868e96',
      'gray-darkest': '#1b1c1e',
      'gray-darker': '#36393c',
      'gray-dark': '#6b7278',
      'gray-light': '#aab0b6',
      'gray-lighter': '#dbdde0',
      'gray-lightest': '#f3f4f5',


      'gray-dark-darkest': '#0a0c0d',
      'gray-dark-darker': '#15171a',
      'gray-dark-dark': '#2a2e33',
      'gray-dark-light': '#717579',
      'gray-dark-lighter': '#c2c4c6',
      'gray-dark-lightest': '#ebebec',

      'gray-100': '#E8E9E9',
      'gray-200': '#D1D3D4',
      'gray-300': '#BABDBF',
      'gray-400': '#808488',
      'gray-500': '#666A6D',
      'gray-600': '#4D5052',
      'gray-700': '#333537',
      'gray-800': '#292b30',
      'gray-900': '#1C1D1E'
    }
  };

})(jQuery, window, document);
