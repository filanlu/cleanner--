@echo off
REM OpenClaude launcher for data-wiper project
set CLAUDE_CODE_USE_OPENAI=1
set OPENAI_BASE_URL=https://token-plan-cn.xiaomimimo.com/v1
set OPENAI_API_KEY=tp-c5kjer0fg4mxjpsu8awiv9w66m97sh7vwktoeypy588dmgai
set OPENAI_MODEL=mimo-v2.5-pro

cd /d "%~dp0"
node C:\Users\19851\Downloads\openclaude-0.14.0\openclaude-0.14.0\dist\cli.mjs %*
