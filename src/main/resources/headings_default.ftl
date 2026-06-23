# ${title}

<#list headings as h>
<#assign d = h.level + 1><#t>
<#if d gt 6><#assign d = 6></#if><#t>
<#list 1..d as i>#</#list> ${h.title}  *(Location: ${h.location})*
</#list>
