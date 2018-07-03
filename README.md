# IntelliJ Inspection Plugin for Google™ Closure
A plugin which enhances IntelliJ's support for Google™ Closure JavaScript by 

* checking for **missing** `goog.require` statements :eyeglasses:
* checking for **obsolete** `goog.require`s :eyeglasses:
* checking for **duplicate** goog.require/goog.provide statements
* checking for usages of **bracket notation** (e.g. `myVar['myField']` instead of dot notation (`myVar.myField`). (The first alternative cannot be type-checked by the Closure compiler)
* checking for **swapped type annotation & parameter name** in JSDoc (correct order: `@param {string} myParameter`)
* offering quick-fixes :star2: (incl. **fix all**) for all of the above
* sorting `goog.require` statements automatically when quick fixes are invoked:

![Inspection GIF](https://github.com/Dan1ve/ClosureInspectionsPlugin/raw/master/images/require-fixes.gif)

## Installation

Simply install the plugin via directly in IntelliJ (`Plugins` > `Browse repositores`), which is linked to this site: https://plugins.jetbrains.com/plugin/10725-inspections-for-google-closure

## Note 

Please note that this plugin does only infer dependencies and assumes that the Closure naming conventions for JavaScript are used. It does _not_ run the Closure compiler, so there can be small dependency differences.

## License 

```
Copyright (C) 2018 Daniel Veihelmann

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
