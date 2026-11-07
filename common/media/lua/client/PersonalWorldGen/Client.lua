PersonalWorldGen = PersonalWorldGen or {}

local function api()
    if not PersonalWorldGenAPI then return nil end
    return PersonalWorldGenAPI.getInstance()
end

Events.OnGameStart.Add(function()
    local service = api()
    if not service then
        print("[PersonalWorldGen] API unavailable")
        return
    end
    print("[PersonalWorldGen] " .. tostring(service:status()))
    print("[PersonalWorldGen] demo " .. tostring(service:planDemo(42)))
end)
