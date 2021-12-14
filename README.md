# SVHSI - Secure and Verified Smart Home Infrastructure

<img src="logo.png" alt="logo" width="200"/>

![CI](https://github.com/dslab-epfl/smartinfra/actions/workflows/ci.yml/badge.svg)

- [SVHSI - Secure and Verified Smart Home Infrastructure](#svhsi---secure-and-verified-smart-home-infrastructure)
  - [Installation](#installation)
  - [Supported devices](#supported-devices)
  - [Developing an application](#developing-an-application)
  - [Running the applications](#running-the-applications)
  - [App generator](#app-generator)
    - [Prototypical structure](#prototypical-structure)
    - [Usage](#usage)
  - [CLI](#cli)
  - [Contributing](#contributing)
  - [License](#license)

The SVSHI (**S**ecure and **V**erified **S**mart **H**ome **I**nfrastructure) project is about developing a platform/runtime/toolchain for developing and running formally verified smart infrastructures, such as smart buildings, smart cities, etc.

It provides a CLI to interact easily with the platform.

With SVHSI, a user can develop and run Python applications interacting with [KNX](https://www.knx.org/knx-en/for-professionals/index.php) systems that are formally verified at both compile- and run-time against a set of provided invariants.

## Installation

To work, SVHSI needs Python 3.8 or newer and Java 11 or newer.

1. Clone the repo
2. Run `./install.sh`
3. Add `$HOME/local/bin` (where the CLI executable is stored) to the path by adding `export PATH=$HOME/local/bin:$PATH` to your `.bash_profile`, `.zsh_profile`, etc.
4. Add the variable `SVSHI_HOME` to your environment by adding `export SVSHI_HOME=path/to/your/cloned/repo` to your `.bash_profile`, `.zsh_profile`, etc.

To check if the installation was successful, run `svshi version` in your terminal.

## Supported devices

- **Binary sensors** (deviceType = "binary")
- **Temperature sensors** (deviceType = "temperature")
- **Humidity sensors** (deviceType = "humidity")
- **Switches** (deviceType = "switch")

## Developing an application

To develop an app for Pistis:

1. Create the devices prototypical structure file containing the list of the devices the app should use, as explained in the [app prototypical structure](#prototypical-structure) section.
2. Run the app generator, as explained in the [app generator](#app-generator) section, to get the app skeleton. It will be created under the `generated/` folder.
3. Run svshi to generate the bindings with `svshi generateBindings -f ets.knxproj"`, where the argument is the _absolute_ path to the ETS project file.
4. Map the right physical ids given in `generated/physical_structure.json` to the right device in `generated/apps_bindings.json`. This is needed to provide the devices in the Python code with the group addresses to use. The first file represents the physical structure from the ETS project file, where each communication object has an id. The second one represents the apps structure with the devices and for each of them, the links they need.
5. Write your app.
6. Run svshi again to compile and verify the app with `svshi compile -f ets.knxproj"`.

## Running the applications

To run all the installed apps (with runtime verification enabled):

1. In [ETS](https://www.knx.org/knx-en/for-professionals/software/ets-professional/), assign to each communication object the right group address as presented in `assignments/assignment.txt`.
2. Execute `svshi run`.

## App generator

### Prototypical structure

This JSON file is given by the programmer/developer that wants to develop an application. It represents the prototypical devices that the app needs with their types. It also specifies whether the app is _privileged_ or not (`"permissionLevel": "privileged" | "notPrivileged"`). A privileged app can override the behavior of the non-privileged ones.

Once the app is generated, it is moved to the `generated` folder.

Here is an example:

```json
{
  "permissionLevel": "notPrivileged",
  "devices": [
    {
      "name": "name_of_the_instances",
      "deviceType": "type_of_the_devices"
    }
  ]
}
```

The `name` is used as the instance name in the Python app that is generated. It should then be unique in a given app, and should follow the Python variables naming conventions: no whitespaces nor numbers.
The `deviceType` should be [supported by SVSHI](#supported-devices).

### Usage

The app generator takes as input the devices JSON file and the name of the app.

Before executing it, you need to create the file containing the list of the devices the app should use, as explained [above](#prototypical-structure).

To execute the generator, run `svshi generateApp -d devices.json -n app_name`, where the first argument is the _absolute_ path to the prototypical structure file and the second one (`app_name` in the example) is the name of the app to be used. The name has to follow the same rules as for Python modules: short, all-lowercase names. Underscores can be used if it improves readability.

## CLI

You can run `svshi --help` to display the following:

```
svshi
Secure and Verified Smart Home Infrastructure
  -t task <command>    The task to run. Can be passed without the flag. Possible options are 'run', 'compile', 'generateBindings', 'generateApp', 'listApps' and 'version'
  -f --ets-file <str>  The ETS project file to use for the tasks 'compile' and 'generateBindings'
  -n --app-name <str>  The app name to use for the task 'generateApp'
  --no-colors          The flag to disable output coloring
```

Available commands are:

- `svshi run` to run all the apps with runtime verification
- `svshi compile -f ets.knxproj` to compile all the apps
- `svshi generateBindings -f ets.knxproj` to generate the bindings for all the apps
- `svshi generateApp -d devices.json -n app_name` to generate a new Python app
- `svshi listApps` to list all the installed apps
- `svshi version` to display the CLI version

## Contributing

See [the contributing guide](/CONTRIBUTING.md) for detailed instructions on how to get started with our project.

## License

The project is licensed under the [MIT license](/LICENSE).
