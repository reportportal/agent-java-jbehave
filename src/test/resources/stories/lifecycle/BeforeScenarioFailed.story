Lifecycle:
Before:
Scope: STORY
When I have one more empty step
Scope: SCENARIO
Given I have a failed step
Scope: STEP
Given It is test with parameters
After:
Scope: STEP
When I have parameter test
Scope: SCENARIO
Then I have another empty step
Scope: STORY
Given It is a step with an integer parameter 42

Scenario: The scenario

Given I have empty step
