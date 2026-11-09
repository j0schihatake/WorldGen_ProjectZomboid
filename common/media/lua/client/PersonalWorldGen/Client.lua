PersonalWorldGen = PersonalWorldGen or {}

local function api()
    if not PersonalWorldGenAPI then return nil end
    return PersonalWorldGenAPI.getInstance()
end

local page
local started = false

pcall(function()
    local ok = require("PZAPI/ModOptions")
    if ok and PZAPI and PZAPI.ModOptions then
        page = PZAPI.ModOptions:getOptions("PersonalWorldGen")
        if not page then
            page = PZAPI.ModOptions:create("PersonalWorldGen", "PersonalWorldGen")
        end
        if page and not page:getOption("placeDebugMarker") then
            page:addTickBox("placeDebugMarker", "Debug marker", false,
                "Place one world item to verify the generation pipeline")
        end
    end
end)

local function ensurePage()
    pcall(function()
        local ok = require("PZAPI/ModOptions")
        if ok and PZAPI and PZAPI.ModOptions then
            page = PZAPI.ModOptions:getOptions("PersonalWorldGen")
            if not page then
                page = PZAPI.ModOptions:create("PersonalWorldGen", "PersonalWorldGen")
            end
            if page and not page:getOption("placeDebugMarker") then
                page:addTickBox("placeDebugMarker", "Debug marker", false,
                    "Place one world item to verify the generation pipeline")
            end
        end
    end)
end

local function start()
    if started then return true end
    ensurePage()
    local service = api()
    if not service then return false end
    started = true
    print("[PersonalWorldGen] " .. tostring(service:status()))
    print("[PersonalWorldGen] demo " .. tostring(service:planDemo(42)))
    print("[PersonalWorldGen] pattern " .. tostring(service:patternDemo()))
    print("[PersonalWorldGen] library " .. tostring(service:patternLibraryDemo()))
    print("[PersonalWorldGen] engine " .. tostring(service:patternEngineDemo()))
    print("[PersonalWorldGen] ops " .. tostring(service:patternOpsDemo()))
    print("[PersonalWorldGen] regions " .. tostring(service:regionRegistryDemo()))
    print("[PersonalWorldGen] structures " .. tostring(service:structurePatternsDemo()))
    print("[PersonalWorldGen] variants " .. tostring(service:structureVariantsDemo()))
    print("[PersonalWorldGen] landmarks " .. tostring(service:structureLandmarksDemo()))
    local player = getSpecificPlayer(0)
    if player then
        local data = player:getModData()
        service:restoreGenerated(tostring(data.PersonalWorldGenRegions or ""))
        if getGameTime and getGameTime():getWorldAgeHours() < 1
                and not string.find(service:generatedRegions(), "start-site", 1, true) then
            print("[PersonalWorldGen] auto " .. tostring(service:generateStart(player)))
            data.PersonalWorldGenRegions = tostring(service:generatedRegions())
        end
        print("[PersonalWorldGen] near " .. tostring(service:planNearPlayer(player, 42)))
        local lastSave = 0
        local saveTick
        saveTick = function()
            local now = getTimestampMs()
            if now - lastSave >= 60000 then
                lastSave = now
                data.PersonalWorldGenRegions = tostring(service:generatedRegions())
            end
        end
        Events.OnTick.Add(saveTick)
    end
    return true
end

Events.OnGameStart.Add(function()
    if start() then return end
    local handler
    handler = function()
        if start() then Events.OnPlayerUpdate.Remove(handler) end
    end
    Events.OnPlayerUpdate.Add(handler)
end)
