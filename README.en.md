English / [Chinese](README.md)

## about

Neatlogic-autoexec is an automated management module that comes with customization tools, combined tool, job management, and other functions.

## Feature

### Tools

Tools are divided into two types: system built tools and custom tools. The execution methods of the tools support local execution (runner) and execution on the target machine (target, runner ->target)<br>
![img.png](README_IMAGES/img.png)
A variety of script parsers are built into the custom tool, supporting commonly used script types such as python, javascript, perl, and sh, and supporting the definition of input and input parameters. Default values can be configured for parameter values.

### Combined tool

Based on a combined script of tools and custom tools, add tools to the stage and complete tool configuration by creating a stage framework.
![img.png](README_IMAGES/img1.png)
-Support the definition of job parameters, which can be referenced by tool parameters and execution targets
-Support for preset execution goals in stages or stage groups, as well as for the entire job. Stage execution goals have a higher priority.

### Job

Initiate automated jobs based on composite tools
![img.png](README_IMAGES/img2.png)
On the job management page, you can filter target jobs based on needs and view job details
![img.png](README_IMAGES/img3.png)