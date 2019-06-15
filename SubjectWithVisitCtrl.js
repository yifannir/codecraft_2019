import React, { Component } from 'react';
import {Route, Link, Redirect, Switch,BrowserRouter,Prompt} from 'react-router-dom';
import { Modal } from "antd";
import VisitManager from "./VisitManager"
import MenuAndContentLayout from '../../../layout/MenuAndContentLayout';
import MenuSupportsVisitCtrl from "../visitcontrol/MenuSupportsVisitCtrl"
import ReportCardManager from "../ReportCardManager";
import InProgressOnLeaveConfirm from "./InProgressOnLeaveConfirm";
import pic from "../../../resources/intro/pic.jpg"
const horn = require("../../horn/horn");

function ModalFor3SecondsDisplay(onClose) {
    let secondsToGo = 2;


    const modal = Modal.success({
        //title: '临时对话框',
        width:700,
        content:
            <div>
                <img src={pic} width="100%" height="100%"  alt=""/>
            </div>,
        icon :null,
        okText :"确定",
        mask:true,
    });
    const timer = setInterval(() => {
        secondsToGo -= 1;
        modal.update({
            content:
                <div>
                    <img src={pic} width="100%" height="100%" alt=""/>

                </div>,
            icon :null,

            
        });
    }, 1000);

    setTimeout(() => {
        clearInterval(timer);
        modal.destroy();

        if (onClose)
            onClose();

    }, secondsToGo * 1000);
}



function VisitControlListener(owner) {
    this.owner = owner;
    this.handleMsg = msg => {
        if(msg.type === "GuideAction"){
            switch (msg.action) {
                case "VisitIntro":
                    break;
                case "VisitSection":
                    this.owner.handleVisitSection(msg.detail.name);
                    break;
                case "VisitExperiment":
                    this.owner.handleVisitExperiment(msg.detail.id);
                    break;
                case "VisitExercise":
                    this.owner.handleVisitExercise(msg.detail.id);
                    break;
                case "CompleteExperiment":
                    this.owner.handleCompleteExperiment(msg.detail.id);
                    break;
                case "CompleteExercise":
                    this.owner.handleCompleteExercise(msg.detail.id);
                    break;
            }
        }
    }
}

/**
 * props:
 *
 * subjectId    需要展示的subject的id
 */
class SubjectWithVisitCtrl extends Component {
    constructor(props){
        super(props);

        this.visitCtrlHorn = new horn();
        this.visitCtrlHorn.addListener(new VisitControlListener(this));

        this.reportCardMng = new ReportCardManager(this.visitCtrlHorn);

        this.visitCtrl = new VisitManager(this.props.subjectId, this.visitCtrlHorn);
        this.visitCtrl.init();

        let routeInfo = this.visitCtrl.getSubjectRouteInfo();
        this.state = {
            routeInfo: routeInfo,
            redirects: this.getRedirects({subjectRouteInfo: routeInfo}),
            routedComponents : this.visitCtrl.getRoutedComponents(),
        };

    }

    componentDidMount() {
        this.visitCtrl.handleVisitSubject(
            ()=>{
                ModalFor3SecondsDisplay(
                    ()=>{this.jumpTo("/intro")}
                )
            }
        );
        this.updateVisitCtrl();
    }

    handleVisitSection = (name) => {
        let sectionId = undefined;
        let introPath = undefined;
        let sections = this.state.routeInfo.sections;
        for (let i=0;i<sections.length;i++){
            if (sections[i].name === name){
                sectionId = sections[i].id;
                introPath = sections[i].intro.path;
                break;
            }
        }

        if (sectionId && introPath){
            this.visitCtrl.handleVisitSection(
                sectionId,
                ()=>{
                    this.jumpTo(introPath);
                    this.updateVisitCtrl();
                }
            );
        }
    };


    handleVisitExperiment = (id) => {
        this.visitCtrl.handleVisitExperiment(id);
        this.updateVisitCtrl();
    };

    handleCompleteExperiment = id => {
        this.visitCtrl.handleCompleteExperiment(id);
        this.updateVisitCtrl();
    };

    handleVisitExercise = id => {
        this.visitCtrl.handleVisitExercise(id);
        this.updateVisitCtrl();
    };

    handleCompleteExercise = id => {
        this.visitCtrl.handleCompleteExercise(id);
        this.updateVisitCtrl();
    };

    updateVisitCtrl = () => {
        let routeInfo = this.visitCtrl.getSubjectRouteInfo();

        this.setState({
            routeInfo: routeInfo,
            redirects: this.getRedirects({subjectRouteInfo: routeInfo})
        })
    };

    /**
     * 为sectionsRouteInfo 中disabled 为true 的项生成指向 "/" 的跳转
     *
     * @param subjectRouteInfo
     * @returns {Array}
     */
    getRedirects = ({subjectRouteInfo}) => {
        let sectionsRouteInfo = subjectRouteInfo.sections;

        let redirects = [];

        let sections = sectionsRouteInfo;
        for (let i=0;i<sections.length;i++){
            let section = sections[i];

            if (section.intro.disabled)
                redirects.push(routeRedirect(
                    section.intro.path,
                    "/"
                ));

            let exps = section.experiments;
            for (let i=0;i<exps.length;i++){
                if (exps[i].disabled){
                    redirects.push(routeRedirect(
                        exps[i].path,
                        "/"
                    ))
                }
            }

            let excs = section.exercises;
            for (let i=0;i<excs.length;i++){
                if (excs[i].disabled){
                    redirects.push(routeRedirect(
                        excs[i].path,
                        "/"
                    ))
                }
            }
        }
        return redirects;
    };

    render() {
        return (
            <InProgressOnLeaveConfirm
                horn={this.visitCtrlHorn}
                onConfirm={()=>{this.setState({
                    routedComponents: this.visitCtrl.getRoutedComponents()
                })}}
            >
                <MenuAndContentLayout
                    Menu={
                        <MenuSupportsVisitCtrl
                            subjectRouteInfo={this.state.routeInfo}
                            onMenuSelect={this.handleVisitSection}
                        />
                    }
                    Content={
                        <div>
                            <Jumper ref={
                                (e)=>{
                                    if (e !== null){
                                        this.jumpTo = e.jumpTo;
                                    }
                                }}
                            />
                            <Switch>
                                {this.state.redirects}
                                <Route exact path="/"
                                       component={Home}>
                                </Route>
                                {/*<Route path="/debugger"*/}
                                       {/*component={SimplifiedDebugUI_layout2}>*/}
                                {/*</Route>*/}
                                {this.state.routedComponents}
                            </Switch>
                        </div>
                    }
                />
            </InProgressOnLeaveConfirm>
        );
    }
}

class Jumper extends Component{
    constructor(props){
        super(props);

        this.state = {
            target: undefined
        }

    }

    jumpTo = path => {
        this.setState({
            target: path
        })
    };

    render() {
        if (this.state.target !== undefined){
            this.setState({
                target: undefined
            })
        }

        if (this.props.onComplete){
            this.props.onComplete();
        }


            return(
            this.state.target === undefined ?
                <div></div>
                :
            <Redirect
                from={"/"}
                to={this.state.target}
            />
        )
    }
}

class Home extends Component{
    render(){
        return (
            <div>
                <div style={{height: '100px'}}/>
                <div>欢迎访问OnlineCoder 0.2.7.1</div>
            </div>
        )
    }
}

function routeRedirect(from, to) {
    return(
        <Redirect
            exact
            from={from}
            to={to}
        />
    )
}

export default SubjectWithVisitCtrl;
