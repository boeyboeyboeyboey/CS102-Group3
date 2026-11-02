(() => {
    'use strict';
    const search = document.getElementById('search');
    const status = document.getElementById('status');
    const sections = Array.from(document.querySelectorAll('section.file'));
    const methods = Array.from(document.querySelectorAll('.method'));
    const changed = new Set(['MODIFIED', 'ADDED', 'REMOVED']);
    function filter() {
        const query = search.value.trim().toLowerCase();
        let visible = 0;
        sections.forEach(section => {
            const label = section.querySelector('h2').textContent.toLowerCase();
            let matches = 0;
            section.querySelectorAll('.method').forEach(method => {
                const heading = method.querySelector('summary');
                const text = label + ' ' + (heading ? heading.textContent.toLowerCase() : '');
                const state = method.dataset.status;
                const acceptsStatus = status.value === 'ALL' || state === status.value ||
                    (status.value === 'CHANGED' && changed.has(state));
                method.hidden = !(acceptsStatus && text.includes(query));
                if (!method.hidden) matches++;
            });
            section.hidden = matches === 0;
            visible += matches;
        });
        document.getElementById('count').textContent = visible + ' of ' + methods.length + ' entries shown';
        document.getElementById('empty').hidden = visible !== 0;
    }
    search.addEventListener('input', filter);
    status.addEventListener('change', filter);
    document.getElementById('expand').addEventListener('click', () => {
        methods.forEach(method => { if (!method.hidden && method.tagName === 'DETAILS') method.open = true; });
    });
    document.getElementById('collapse').addEventListener('click', () => {
        methods.forEach(method => { if (method.tagName === 'DETAILS') method.open = false; });
    });
    function revealTarget() {
        const target = document.getElementById(window.location.hash.slice(1));
        if (!target || !target.matches('section.file')) return;
        search.value = '';
        status.value = 'ALL';
        filter();
        target.querySelectorAll('details.method').forEach(method => { method.open = true; });
        target.querySelector('h2').focus({ preventScroll: true });
        target.scrollIntoView({ block: 'start' });
    }
    document.querySelectorAll('a.why').forEach(link => {
        link.addEventListener('click', event => {
            if (event.button !== 0 || event.ctrlKey || event.metaKey || event.shiftKey || event.altKey) return;
            event.preventDefault();
            window.location.hash = link.hash;
            revealTarget();
        });
    });
    window.addEventListener('hashchange', revealTarget);
    filter();
    revealTarget();
})();
