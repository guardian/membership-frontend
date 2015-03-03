# CSS at Guardian Membership

---

**Table of contents**

* [Architecture](#architecture)
* [Naming Conventions](#naming)
* [BEM Recommendations](#bem)
* [JavaScript](#javascript)
* [Vendor Prefixes](#prefixes)
* [Further Reading](#reading)

<a name="architecture"></a>
## Architecture

We use Sass for our stylesheets and break styles down into the following groups:

1. **Settings**: Global variables (colours, breakpoints etc.)
2. **Mixins**: Functions and mixins
3. **Base**: Un-classed HTML elements and resets (normalize)
4. **Helpers**: Common design patterns and traits (grid, text, heading traits)
5. **Components**: Styled objects, chunks of UI
5. **Overrides (trumps)**: Helpers and overrides, (utilities class, responsive visibility helpers, colours).

<a name="naming"></a>
## Naming conventions:

We largely use the following naming conventions in our CSS:

- We (sparingly) use low-level helper classes for commmon traits, e.g., headings, utilities
- We use a version of BEM to distinguish between components
- We use state classes (e.g., `is-toggled`) to style components affecteder by JavaScript

## Helpers

Any low-level helper classes or traits use a set of common prefixes. Where possible helper classes should be a single class with no children.

- `.l-` for layout helpers
- `.h-` for heading traits
- `.text-` for text helpers
- `.grid-` for grid helpers
- `.u-` for utilities

### Layout helpers

Layout helpers are common traits used to extract out common layout traits, for example constraining widths (`l-constrained`) or insetting content (`l-pad`).

### Text traits

Common text traits are described using `h-` prefixed classes for headings and `text-` prefixed classes for broader text styling traits.

These allow us to abstract out the presentation of text into a common design language. We still style text on a per-component basis as needed but these helper classes allow us, for example, to have a single place where featured text, or the styles for a caption are defined.

### Grid

The grid helpers provide a common pattern for setting up 2up, 3up and 4up grids. It's not a full grid system but provides a useful abstraction for common equal-width grids. See the events listing pages as an example.

### Utilities

Utilities classe (prefixed with `u-`) are patterns for common overrides, they are included at the end of the stylesheet and typically include `!important` declarations as they act as style trumps. Typical utilities include clearfixes (`u-cf`) and margin resets (`u-no-margin`).


## Components

Syntax: `<componentName>[--modifierName|-descendantName]`

Component driven development offers several benefits when reading and writing HTML and CSS:

* It helps to distinguish between the classes for the root of the component, descendant elements, and modifications.
* It keeps the specificity of selectors low.
* It helps to decouple presentation semantics from document semantics.

You can think of components as custom elements that enclose specific semantics, styling, and behaviour.


### Component Names

Name with type of component first, so rather than `.inline-list`, `.stacked-list`, prefer `.list-stacked`, `.list-inline`. Hyphenate multi-word component name.

```
.my-component { /* */ }
```

```
<article class="my-component">
</article>
```

### Component Modifiers

A component modifier is a class that modifies the presentation of the base component in some form. Modifiers should be separated from the component name by two hyphens. The class should be included in the HTML _in addition_ to the base component class.

```
/* Core button */
.action { }
/* Booking button style */
.action--booking { /* … */ }
```

```
<button class="action action--booking">Action</button>
```
### Component Decendants

A component descendant is a class that is attached to a descendant node of a component. It's responsible for applying presentation directly to the descendant on behalf of a particular component. Decendants should be separated from the component name by two underscores.

```
<article class="tweet">
  <header class="tweet__header">
    <img class="tweet__avatar" src="{$src}" alt="{$alt}">
  </header>
  <div class="tweet__body">
  </div>
</article>
```

### State Classes

Use `is-state-name` for state-based modifications of components. The state name should be lowercase with multiple words separated with hyphens. **Avoid styling these classes directly; they should be used as an adjoining class.**

JS can add/remove these classes. This means that the same state names can be used in multiple contexts, but every component must define its own styles for the state (as they are scoped to the component).

```
.tweet { /* … */ }
.tweet.is-expanded { /* … */ }
```

```
<article class="tweet is-expanded">
  …
</article>
```

<a name="bem"></a>
## BEM Recommendations

### Keep selector level to minimum

Heavily nested BEM selectors (e.g., `.event__sidebar__tickets__content`) are a sign you need to break a component out into smaller pieces.

**Rule-of-thumb**: Three or more levels deep and you should be thinking about splitting up components.

### Unintended modifiers

Try to avoid modifiers that don’t have a root class e.g., if you have `list--stacked`, `list--inline` you must also have a `list` class. If you don’t then these are two separate components with a common name `list-stacked` and `list-inline`.

### Avoid modifiers on children

Modifiers on children can feel correct in BEM notation, after all it keeps the specificity low (one level deep), but still allows modification of child elements. However it can often cause a conceptual disconnect, as it’s not immediately clear where the modifier is intended to be used.

Instead try to keep modifiers to the root level selector. For example given the following component:

```
.mod { color: hotpink }
.mod__child { padding: 2%; }

Instead of
.mod__child--slim { padding: 1% }

Prefer:

.mod--slim {
   .mod__child {
      padding: 1%;
   }
}
```

While the former keeps specificity as low as possible, it’s easier to reason about the second level as we are documenting a known set of variants for a component. If you are consistently adding modifiers to child elements it’s a good indicator that the component is too large; that element should probably be the root of a new component.

### Don’t use BEM children out of context.

One risk with BEM naming conventions is that it’s possible to use a child-selector outside of the context of its original parent. So if you see something like the following then it’s a sign that your component wasn’t granular enough or you need to refactor your views:

```
<article class="some-component">
	<h2 class="some-component__title"">Title<h2>
  <p class="urelated-component__child"></p>
</article>
```

<a name="javascript"></a>
## JavaScript

> Separate style and behavior concerns by using `.js-` prefixed classes for behavior.

**`.js-` classes should never appear in your stylesheets.** They are for JavaScript only. Inversely, there is rarely a reason to see presentation classes like `.header-nav-button` in JavaScript. You should use state classes like `.is-state` in your JavaScript and your stylesheets as `.component.is-state`.

<a name="prefixes"></a>
## Vendor Prefixes

We use [Autoprefixer](https://github.com/postcss/autoprefixer) for vendor prefixes so write any prefixed CSS using the spec version only and the build process will add any requried prefixes. It's suprising how few vendor prefixes you need anymore.

<a name="reading"></a>
## Further reading

- CSS Guidelines: [http://cssguidelin.es/]()
- MindBEMding – getting your head ’round BEM syntax: [http://csswizardry.com/2013/01/mindbemding-getting-your-head-round-bem-syntax/]()
- Objects in space: [https://medium.com/objects-in-space/objects-in-space-f6f404727]()
- CSS for grownups: [https://www.youtube.com/watch?v=ZpFdyfs03Ug]()
- ITCSS talk: [https://www.youtube.com/watch?v=1OKZOV-iLj4&hd=1]()
- SMACSS: [https://smacss.com/]()
