import {GameState} from "../GameScreen";
import {useState, useEffect} from "react";
import {JsDuck} from '../../models/Duck';
import axios from "axios";
import DuckContainer from "../DuckContainer";

type MainActionsScreenProps = {
    gameStateSetter: (gs: GameState) => void
}

const MAX_DUCKS_SLOTS = 6
const LIST_MODE = "List"
const SET_MODE = "Set"
const MAP_MODE = "Map"

export const PARTITION_FUNCTION = "partition"

export default function MainActionsScreen({gameStateSetter}: MainActionsScreenProps) {
    let [ducks, ducksSetter] = useState<Array<JsDuck>>([])
    let [mode, modeSetter] = useState<String>("")
    let [pressedFunction, pressedFunctionSetter] = useState<String>("")
    let [infoText, infoTextSetter] = useState<String>("")

    // On load, restore the collection persisted on the server (survives restarts).
    useEffect(() => {
        axios.get("/ducks/state").then((response) => {
            const state = response.data as { mode: string, ducks: Array<JsDuck> }
            if (state.ducks && state.ducks.length > 0) {
                ducksSetter(state.ducks)
                modeSetter(state.mode)
            }
        }).catch(() => { /* no saved state — start empty */ })
    }, [])

    function initDuckShop(mode: string) {
        axios.put("/ducks", null, {params: {mode}}).then((response) => {
            ducksSetter(response.data as Array<JsDuck>)
        })
    }

    function initListOfDucks() {
        initDuckShop(LIST_MODE)
        modeSetter(LIST_MODE)
        infoTextSetter("")
        pressedFunctionSetter("")
    }

    function initSetOfDucks() {
        initDuckShop(SET_MODE)
        modeSetter(SET_MODE)
        infoTextSetter("")
        pressedFunctionSetter("")
    }

    function initMapOfDucks() {
        initDuckShop(MAP_MODE)
        modeSetter(MAP_MODE)
        infoTextSetter("")
        pressedFunctionSetter("")
    }

    function wasGameInitialized() {
        return ducks.length != 0;
    }

    function canAddDuck() {
        return ducks.length < MAX_DUCKS_SLOTS
    }

    function canRemoveDuck() {
        return ducks.length > 0
    }

    function applyResponse(response: any, info: string, pressed: string = "") {
        ducksSetter(response.data as Array<JsDuck>)
        pressedFunctionSetter(pressed)
        infoTextSetter(info)
    }

    function shuffleDucks() {
        axios.patch("/ducks", {order: "random"}).then((response) => {
            applyResponse(response, "Ducks have been \nshuffled randomly!")
        })
    }

    function sortDucks() {
        axios.patch("/ducks", {order: "price"}).then((response) => {
            applyResponse(response, "Ducks have been sorted \naccording to the price of the stuff!")
        })
    }

    function partitionDucks() {
        if (!ducks.some((duck) => duck.hasKotlinAttribute)) {
            alert("Sorry, ducks with Kotlin stuff haven’t been found!")
            return
        }
        axios.patch("/ducks", {order: "kotlin-first"}).then((response) => {
            applyResponse(
                response,
                "Ducks with Kotlin stuff have been moved \nto the beginning of the collection!",
                PARTITION_FUNCTION
            )
        })
    }

    function filterDuck() {
        if (!ducks.some((duck) => duck.hasKotlinAttribute)) {
            alert("Sorry, ducks with Kotlin stuff were not found!")
            return
        }
        axios.delete("/ducks", {params: {hasKotlin: false}}).then((response) => {
            applyResponse(response, "Only ducks with Kotlin \nstuff have been left!")
        })
    }

    function addDuck() {
        axios.post("/ducks").then((response) => {
            applyResponse(response, "A new random duck has been \ngenerated successfully!")
        })
    }

    function removeDuck() {
        const index = Math.floor(Math.random() * ducks.length)
        axios.delete("/ducks/" + index).then((response) => {
            let parsedDucks = response.data as Array<JsDuck>
            ducksSetter(parsedDucks)
            pressedFunctionSetter("")
            infoTextSetter("A random duck has been \nremoved successfully!")
            if (parsedDucks.length == 0) {
                modeSetter("")
                infoTextSetter("")
            }
        })
    }

    const BASE_BUTTON_COLLECTION_CLASSES = "App-button-base App-button-collection"
    const BASE_BUTTON_ACTION_CLASSES = "App-button-base App-button-action"

    return (
        <div className="App-main-container">
            <div className="App-buttons-container">
                <button className="App-button-base App-game-button-bottom-base App-button-back" onClick={() => {
                    gameStateSetter(GameState.START)
                    infoTextSetter("")
                }
                }></button>
            </div>
            {
                wasGameInitialized() ?
                    <div>
                        <div className="App-info-container">
                            <div className="App-info-container-text font-link-base">{infoText}</div>
                        </div>
                        <div className="App-functions-container">
                            <button
                                className={"App-button-base App-button-action App-button-add " + (canAddDuck() ? "" : "App-unclickable-button")}
                                onClick={() => addDuck()}></button>
                            <button
                                className={"App-button-base App-button-action App-button-remove" + (canRemoveDuck() ? "" : "App-unclickable-button")}
                                onClick={() => removeDuck()}></button>
                            <button
                                className={BASE_BUTTON_ACTION_CLASSES + " App-button-sort" + (mode == LIST_MODE ? "" : " App-unclickable-button")}
                                onClick={() => sortDucks()}></button>
                            <button
                                className={BASE_BUTTON_ACTION_CLASSES + " App-button-shuffle" + (mode == LIST_MODE ? "" : " App-unclickable-button")}
                                onClick={() => shuffleDucks()}></button>
                            <button className={BASE_BUTTON_ACTION_CLASSES + " App-button-filter"}
                                    onClick={() => filterDuck()}></button>
                            <button
                                className={BASE_BUTTON_ACTION_CLASSES + " App-button-partition" + (mode == MAP_MODE ? " App-unclickable-button" : "")}
                                onClick={() => partitionDucks()}></button>
                        </div>
                        <DuckContainer ducks={ducks} pressedFunction={pressedFunction}></DuckContainer>
                    </div>
                    : <div className="App-base-text">
                        <div className="font-link-base">Please initialize the duck shop!</div>
                    </div>
            }
            <div className="App-buttons-container">
                <button
                    className={BASE_BUTTON_COLLECTION_CLASSES + " App-button-list" + (mode == LIST_MODE ? " App-button-list-focused" : "")}
                    onClick={() => initListOfDucks()}></button>
                <button
                    className={BASE_BUTTON_COLLECTION_CLASSES + " App-button-set" + (mode == SET_MODE ? " App-button-set-focused" : "")}
                    onClick={() => initSetOfDucks()}></button>
                <button
                    className={BASE_BUTTON_COLLECTION_CLASSES + " App-button-map" + (mode == MAP_MODE ? " App-button-map-focused" : "")}
                    onClick={() => initMapOfDucks()}></button>
            </div>
        </div>
    );
}
