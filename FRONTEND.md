**High-level front-end architecture principles for membership.theguardian.com**

# CSS

## CSS Architecture

The CSS architecture for **membership.theguardian.com** uses principles borrowed from a number of methodologies: ITCSS, BEM and SMACSS. We use Sass for authoring stylesheets with styles broken up into the following groupings:

1. **Settings**: Global variables (colours, breakpoints etc.)
2. **Mixins**: Functions and mixins
3. **Base**: Un-classed HTML elements and resets (normalize)
4. **Helpers**: Common design patterns and traits (grid, text, heading traits)
5. **Components**: Styled objects, chunks of UI
Theme (optional): Themed components, used for multiple sites with the same layout and components. Can be utilised for multi-site Magento setups.
6. **Overrides (trumps)**: Helpers and overrides, (utilities class, responsive visibility helpers).

## Naming conventions:

We use three main conventions for naming classes in our CSS: low-level helpers are named using a common prefix (e.g., `u-capitalize` or `l-constrained`); components are named using BEM conventions and any classes that determine the state of an element (typically added via JavaScript) are prefixed with either `.is-` or `.has`, for example `.is-toggled`.

### Helpers

Any low-level helper classes or traits use a set of common prefixes.

- `.l-` for layout helpers
- `.h-` for heading traits
- `.text-` for text helpers
- `.grid-` for grid helpers
- `.u-` for utilities

### Components
Components follow a more typical BEM naming convention. For example:

```
.mod {}
.mod__header {}
.mod__footer {}
```

**Recommended reading:** [http://cssguidelin.es/#bem-like-naming]()


## BEM recommendations

### Naming components

Name with type of component first (so rather than `.inline-list`, `.stacked-list`, prefer `.list-stacked`, `.list-inline`)

### Keep selector level to minimum 

Heavily nested BEM selectors (e.g., `.event__sidebar__tickets__content`) are a sign you need to break a component out into smaller pieces.

**Rule-of-thumb**: More than 3 levels deep and you should be thinking about splitting up components.

### Unintended modifiers

Try to avoid modifiers that don’t have a root class e.g., if you have `list--stacked`, `list--inline` you must also have a `list` class. If you don’t then these are two separate components with a common name `list-stacked` and `list-inline`.

### Avoid modifiers on children

Modifiers on children can feel correct in BEM notation, after all it keeps the specificity low (one level deep), but still allows modification of child elements. However it can often cause a conceptual disconnect, as it’s not immediately clear where the modifier is intended to be used.

Instead try to keep modifiers to the root level selector. For example given the following component:

```
.mod { color: hotpink }
.mod__child { padding: 2%; }

Instead of 
.mod__child—slim { padding: 1% }

Prefer:

.mod—slim {
   .mod__child {
      padding: 1%;
   }
}
```

While the former keeps specificity as low as possible, it’s easier to reason about the second level as we are documenting a known set of variants for a component. If you are consistently adding modifiers to child elements it’s a good indicator that the component is too large; that element should probably be the root of a new component. 

### Don’t use BEM children out of context.

One risk with BEM naming conventions is that it’s possible to use a child-selector outside of the context of it’s original parent. So if you see something like the following then it’s a sign that your component wasn’t granular enough or you need to refactor your views:

```
<article “some-component”>
	<h2 class=“some-component__title>Title<h2>
  <p class=“**unrelated-component__child**”></p>
</article>
```

## CSS in JavaScript

Prefix any classes that are intended to be used in JavaScript with `.js`.

Conversely, if you are adding classes from within JavaScript, don’t use component specific selectors. Instead, use state classes (.is-, .has-) to control JS enabled state. For example, take this simplified toggle component. Instead of:

```
var el = ‘.js-toggle’;
el.addEventListener(‘click’, function() {
  el.addClass(‘toggle—active’);
});
```

Prefer a more generic class:

```
// […]
el.addClass(‘is-active’);
// […]
```

And style as folllows:

```
.action-toggle {}
.action-toggle.is-active {}
```

This might seem counter to BEM methodology, but for the same reason we use `js-` prefixed classes by using generic `is-` and `has-` classes from within the JS we reduce coupling JavaScript behaviour with class-names that are used for unrelated presentation.

## Further reading

- CSS Guidelines: [http://cssguidelin.es/]()
- MindBEMding – getting your head ’round BEM syntax: [http://csswizardry.com/2013/01/mindbemding-getting-your-head-round-bem-syntax/]()
- Objects in space: [https://medium.com/objects-in-space/objects-in-space-f6f404727]()
- CSS for grownups: [https://www.youtube.com/watch?v=ZpFdyfs03Ug]()
- ITCSS talk: [https://www.youtube.com/watch?v=1OKZOV-iLj4&hd=1]()
- SMACSS: [https://smacss.com/]()