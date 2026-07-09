${title}
<#list groups as g><#if g.clippings?has_content>
== ${g.heading.title} [${g.heading.location}] ==
<#list g.clippings as c>${c.type}: ${c.content}
</#list></#if></#list>
