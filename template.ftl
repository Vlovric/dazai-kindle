<#-- Example FreeMarker template (Obsidian-ish Markdown) -->
<#-- Exposed variables: title (String), groups (List<TemplateGroup>) -->
# ${title}
<#list groups as g>
${""?left_pad(g.heading.level, "#")} ${g.heading.title} (Location ${g.heading.location})

<#if g.clippings?size gt 0>
<#assign skipNext = false>
<#list 0..(g.clippings?size - 1) as i>
<#if skipNext>
<#assign skipNext = false>
<#else>
<#assign entry = g.clippings[i]>
<#assign hasNext = (i + 1 < g.clippings?size)>
<#if entry.type?lower_case == "highlight">
> [!kindle-quote] ${entry.content}
<#if hasNext && g.clippings[i + 1].type?lower_case == "note">
> ${g.clippings[i + 1].content}
<#assign skipNext = true>
<#else>
>
</#if>

<#elseif entry.type?lower_case == "bookmark">
> [!kindle-bookmark] (Location ${entry.location!"?"})
>

</#if>
</#if>
</#list>
</#if>
</#list>