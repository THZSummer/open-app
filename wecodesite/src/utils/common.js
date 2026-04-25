export const queryParams = param => {
    const reg = new RegExp("(^|&)" + param + "=([^&]*)(&|$)");
    const r = 
        window.location.search.substr(1).match(reg) || 
        window.location.hash
            .substring(window.location.hash.search(/\?/) + 1)
            .match(reg);
    if (r !== null) {
        return decodeURIComponent(r[2]);
    }
    return '';
}

export const openUrl = url => {
    if (!url) return;
    window.open(url, '_blank', 'noopener,noreferrer');
}